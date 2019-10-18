import txaio
txaio.use_twisted()

import os
import uuid
import copy
import csv
from datetime import datetime, timedelta
from autobahn.util import utcstr, utcnow
from autobahn.wamp.types import PublishOptions
from txaio import make_logger
import click
import hashlib
import math
import struct


def hl(text, bold=True, color='yellow'):
    if not isinstance(text, str):
        text = '{}'.format(text)
    return click.style(text, fg=color, bold=bold)


def deg2num(lat_deg, lon_deg, zoom):
  lat_rad = math.radians(lat_deg)
  n = 2.0 ** zoom
  xtile = int((lon_deg + 180.0) / 360.0 * n)
  ytile = int((1.0 - math.log(math.tan(lat_rad) + (1 / math.cos(lat_rad))) / math.pi) / 2.0 * n)
  return (xtile, ytile)


def num2deg(xtile, ytile, zoom):
  """
  http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  This returns the NW-corner of the square.
  Use the function with xtile+1 and/or ytile+1 to get the other corners.
  With xtile+0.5 & ytile+0.5 it will return the center of the tile.
  """
  n = 2.0 ** zoom
  lon_deg = xtile / n * 360.0 - 180.0
  lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * ytile / n)))
  lat_deg = math.degrees(lat_rad)
  return (lat_deg, lon_deg)


class Vehicle(object):
    SENSOR_CATEGORIES = {
        'pothole_sensor': uuid.UUID('627f1b5c-58c2-43b1-8422-a34f7d3f5a04'),
        'gps_location': uuid.UUID('2b727dc1-20c2-465a-9cf6-e7ec44966cc2'),
        'rain_sensor': uuid.UUID('46abc7dd-7e76-40c4-be5b-653d35188b3d'),
    }


    log = make_logger()

    def __init__(self, username, samples):
        self._username = username
        self._username_sha3 = hashlib.sha3_256(username.encode()).hexdigest()
        self._samples = samples
        self._cur = 0
        self._counter = 0
        self._repeats = 0

        # opt-in for data sharing
        self._profile = {}

        # when data sharing is active for a sensor category, this holds the seller object
        self._sellers = {}

        # initialize to "no data sharing at all"
        for k in Vehicle.SENSOR_CATEGORIES:
            self._profile[k] = False
            self._sellers[k] = None

    def get_profile(self):
        return self._profile

    def set_profile(self, sensor_category, seller=None):
        assert sensor_category in self._profile
        self._profile[sensor_category] = seller is not None
        self._sellers[sensor_category] = seller

    async def loop(self, session, run_id=None):

        self._cur = (self._cur + 1) % len(self._samples)
        if self._cur == 0:
            self._repeats += 1
            self._counter = 0

        # print every 1min (as we run at 0.5Hz)
        if self._counter % 30 == 0:
            self.log.info(hl('>>>>>>>>>>>> Username {username}, Counter {counter}, Repeats {repeat}, Run {run} <<<<<<<<<<<<<<'.format(username=self._username, counter=self._counter, repeat=self._repeats, run=run_id), color='yellow'))
            info = self.log.info
        else:
            info = self.log.debug

        sample = self._samples[self._cur]
        sample_payload = sample.marshal()

        sample_payload['_run'] = run_id
        sample_payload['_repeat'] = self._repeats
        sample_payload['_counter'] = self._counter

        for sensor_category in self._profile:
            # check if data sharing is enabled for sensor category, and skip to next if not
            if not self._profile[sensor_category]:
                info('Skipped publishing of sensor feed {sensor_category} (data sharing not enabled)', sensor_category=hl(sensor_category))
                continue

            # if data sharing _is_ enabled, we must have been given a seller for the data sharing
            seller = self._sellers.get(sensor_category, None)
            assert seller

            # create the payload for the sensor category, deleting all data from other categories
            payload = copy.deepcopy(sample_payload)
            for k in Vehicle.SENSOR_CATEGORIES:
                if k != sensor_category:
                    del payload[k]

            # publish vehicle location sensor streams
            #
            # encrypted:       audi.vehicle.<sha3_256(username)>.<sensor_category>
            # cleartext: debug.audi.vehicle.<sha3_256(username)>.<sensor_category>

            # encrypt and publish event
            #
            topic = 'audi.vehicle.{}.{}'.format(self._username_sha3, sensor_category)
            api_id = Vehicle.SENSOR_CATEGORIES[sensor_category]

            key_id, enc_ser, ciphertext = await seller.wrap(api_id.bytes, topic, payload)

            await session.publish(topic, key_id, enc_ser, ciphertext, options=PublishOptions(acknowledge=True))
            info('Published {mode} to "{topic}": {payload}', topic=hl(topic, color='green'), payload=payload, mode=hl('Encrypted (XBR)', color='blue'))

            # for debugging/test, additionally publish cleartext event
            #
            topic = 'debug.audi.vehicle.{}.{}'.format(self._username_sha3, sensor_category)

            await session.publish(topic, payload, options=PublishOptions(acknowledge=True))
            info('Published {mode} to "{topic}": {payload}', topic=hl(topic, color='green'), payload=payload, mode=hl('Cleartext', color='red'))

        self._counter += 1


class VehicleEvent(object):
    def __init__(self, zoom=12):
        """

        :param dataset:
        :param zoom: zoom level: [0, 19] https://wiki.openstreetmap.org/wiki/Zoom_levels
        """
        self._zoom = zoom

        self.vehicle_id = None
        self.timestamp = None
        self.lng = None
        self.lat = None
        self.speed = None

        self.rain = None
        self.wiper = None

        self.heading = None
        self.depth = None
        self._depth_faked = None
        self.distance = None

    @staticmethod
    def from_row(row):
        obj = VehicleEvent()

        obj.vehicle_id = 'vehicle{}'.format(row['vehicle'].strip().replace("'", ""))
        time_after_start = float(row['time']) / 1000.
        obj.timestamp = datetime.strptime('2019-03-01', '%Y-%m-%d') + timedelta(seconds=time_after_start)
        obj.lng = float(row['lon'])
        obj.lat = float(row['lat'])
        obj.speed = float(row['speed'])

        # available only in some files
        obj.heading = float(row['heading'])
        obj.distance = str(row['distance']) if row['distance'] else None
        obj.depth = float(row['depth']) if row['depth'] else None

        # if we don't have pothole information in the data, fake it:
        if not obj.depth:
            # just use a pseudo-random and "fixed per event" uniformly distributed value
            random_in = hashlib.sha256('{}:{}'.format(obj.vehicle_id, obj.timestamp).encode()).digest()
            obj.depth = float(struct.unpack('>L', random_in[:4])[0]) / 2 ** 32
            obj._depth_faked = True
        else:
            obj._depth_faked = False

        return obj

    def marshal(self):
        xtile = None
        ytile = None
        if self.lng and self.lat:
            xtile, ytile = deg2num(self.lat, self.lng, self._zoom)

        obj = {
            'ts': utcnow(),
            'vehicle_id': self.vehicle_id,
            'timestamp': utcstr(self.timestamp),
            'gps_location': {
                'lng': self.lng,
                'lat': self.lat,
                'speed': self.speed,
                'heading': self.heading,
                'xtile': xtile,
                'ytile': ytile,
                'zoom': self._zoom
            },
            'rain_sensor': {
                'rain': self.rain,
                'wiper': self.wiper,
            },
            'pothole_sensor': {
                '_depth_faked': self._depth_faked,
                'depth': self.depth,
                'distance': self.distance,
            }
        }
        return obj


def load_samples(filename):
    vehicles = {}
    vehicle_events = None

    fn = os.path.abspath(os.path.join(os.path.dirname(__file__), filename))

    with open(fn, newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        vehicle_events = [VehicleEvent.from_row(row) for row in reader]

    for evt in vehicle_events:
        if evt.vehicle_id not in vehicles:
            vehicles[evt.vehicle_id] = []
        vehicles[evt.vehicle_id].append(evt)

    return vehicles


if __name__ == '__main__':
    vehicles = load_samples('../samples.csv')
    print('Ok, samples loaded for {} vehicle(s)'.format(len(vehicles)))
    for vehicle in vehicles:
        sample_count = len(vehicles[vehicle])
        print('{}: {} samples'.format(vehicle, sample_count))
