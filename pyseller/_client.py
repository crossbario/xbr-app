import os
import json
import argparse
import hashlib
import uuid
from copy import deepcopy

import six
import txaio
from web3 import Web3
from twisted.internet.defer import inlineCallbacks

from autobahn.twisted.util import sleep
from autobahn.twisted.wamp import ApplicationSession, ApplicationRunner
from autobahn.wamp.types import PublishOptions, RegisterOptions, CallDetails
from autobahn.wamp.exception import ApplicationError, TransportLost

from autobahn.xbr import SimpleSeller

import vehicle


class VehicleClient(ApplicationSession):
    """
    Vehicle simulation component (namely WAMP application session class providing services).

    Note: An instance of this class will be created by ApplicationRunner - per WAMP session. When the
    connection is lost and automatically reestablished a _new_ instance of this class will be active.
    """

    def __init__(self, config=None):
        """

        :param config: Component configuration.
        :type config: :class:`autobahn.wamp.types.ComponentConfig`
        """
        ApplicationSession.__init__(self, config)

        self._errors = 0
        self._iterations = 0
        self._skipped = 0
        self._running = False

    async def onJoin(self, details):
        """
        Callback invoked by Autobahn when the router connection has been established and the
        WAMP session joined on the respective realm.

        :param details: Session details.
        :type details: :class:`autobahn.wamp.types.SessionDetails`
        """
        self.log.info('{msg}: realm={realm}, session={session}, authid={authid}, authrole={authrole}, authextra={authextra}',
                      msg=vehicle.hl("VehicleClient.onJoin BEGIN", color='white'),
                      realm=vehicle.hl(details.realm),
                      session=details.session,
                      authid=vehicle.hl(details.authid),
                      authrole=vehicle.hl(details.authrole),
                      authextra=details.authextra)

        self._running = True

        vehicledata = self.config.extra['vehicledata']
        city = 'ingolstadt'  # FIXME: where does this come from?
        run_id = str(uuid.uuid4())

        await self._initialize(details.authid, userdata, vehicledata, city, run_id)

        # Loop forever (every 2s) and run vehicle loops that publish simulated vehicle data
        #
        while self._running:
            if self.is_attached():
                for username, vehicle in self._vehicles.items():
                    try:
                        await vehicle.loop(self, run_id=run_id)
                    except TransportLost:
                        self._errors += 1
                        self.log.warn('TransportLost while looping for vehicle username {username}',
                                      username=vehicle.hl(username))
                    except:
                        self._errors += 1
                        self.log.failure()
            else:
                self._skipped += 1
                self.log.warn('Skipping loop iteration {iteration} - no transport!',
                              iteration=vehicle.hl(self._iterations))

            self._iterations += 1
            await sleep(2)

        self.log.info(vehicle.hl("VehicleClient READY!", color='white'))

    async def _initialize(self, authid, userdata, vehicledata, city, run_id):

        # sha3(username) -> username
        self._usernames_sha3_map = {}

        # map: username -> (map: sensor_category -> seller)
        self._sellers = {}

        APIS = [
            (vehicle.Vehicle.SENSOR_CATEGORIES['rain_sensor'], 'com.example.vehicle.', 35, 60, None),
            (vehicle.Vehicle.SENSOR_CATEGORIES['pothole_sensor'], 'com.example.vehicle.', 50, 30, None),
            (vehicle.Vehicle.SENSOR_CATEGORIES['gps_location'], 'com.example.vehicle.', 15, 20, None),
        ]

        def make_seller(username):
            """
            Create a new XBR simple seller instance.

            :param username:
            :return:
            """
            privkey = Web3.toBytes(hexstr=userdata[username]['privkey'])

            username_sha3 = hashlib.sha3_256(username.encode()).hexdigest()

            seller = SimpleSeller(privkey, provider_id=username_sha3)

            for api_id, prefix, price, interval, categories in APIS:
                seller.add(api_id.bytes, prefix, price, interval, categories)

            seller.start(self)
            return seller

        for username, config in userdata.items():
            self._sellers[username] = make_seller(username)

            username_sha3 = hashlib.sha3_256(username.encode()).hexdigest()
            self._usernames_sha3_map[username_sha3] = username

        self._vehicles = {}
        for username, config in userdata.items():
            dataset, vehicle_id = config['vehicle']
            samples = vehicledata[dataset][vehicle_id]

            username_sha3 = hashlib.sha3_256(username.encode()).hexdigest()

            vehicle = vehicle.Vehicle(username, samples)
            self._vehicles[username] = vehicle

            opt_in = config['opt_in']
            for sensor_category in opt_in:
                if opt_in[sensor_category]:
                    # enable data sharing on the vehicle providing the seller
                    vehicle.set_profile(sensor_category, self._sellers[username])
                else:
                    vehicle.set_profile(sensor_category, None)

            # per-user WAMP procedure: opt-in.<sha3(username).set-profile
            #
            async def set_profile(username_sha3, profile, details=None):
                """
                Set and store new user data sharing opt-in profile and publish a change event.

                .. note::

                    The changeset returned by this procedure (and published as an event) only contains
                    keys for actually changed profile settings.

                :param username_sha3: Hashed username, in HEX (lowercase) encoding of SHA3-256(username)
                :type username_sha3: str

                :param profile: User profile changeset (a dictionary with string keys and boolean values)
                :type profile: dict

                :param details: WAMP call details.
                :type details: :class:`autobahn.wamp.types.CallDetails`

                :returns: The actually applied user profile changes (a dictionary with strings and boolean values)
                :rtype: dict
                """
                assert type(username_sha3) == str
                assert type(profile) == dict
                # profiles must have string keys and boolean values:
                assert (type(key) == str for key in profile)
                assert (type(value) == bool for value in profile.values())
                assert details is None or isinstance(details, CallDetails)

                self.log.info('{method}(username_sha3={username_sha3}) [details]',
                              method=vehicle.hl('set_profile', color='green'),
                              username_sha3=vehicle.hl(username_sha3),
                              details=details)
                if username_sha3 not in self._usernames_sha3_map:
                    raise ApplicationError('road-vehicle.error.no-such-profile')

                username = self._usernames_sha3_map[username_sha3]

                assert username in self._vehicles
                vehicle = self._vehicles[username]

                # set opt-in knobs on user vehicle
                changeset = {}
                current_profile = vehicle.get_profile()
                for key in profile:
                    # we only have these opt-in knobs on vehicles - and ignore others in the profile
                    if key in vehicle.Vehicle.SENSOR_CATEGORIES:
                        if current_profile[key] != profile[key]:
                            changeset[key] = profile[key]

                if changeset:
                    for sensor_category, enabled in changeset.items():
                        if not enabled:
                            vehicle.set_profile(sensor_category, None)
                        else:
                            # enable data sharing on the vehicle providing the seller
                            vehicle.set_profile(sensor_category, self._sellers[username])

                            self.log.info('Data sharing for "{username}" and sensor category "{sensor_category}" started ..',
                                          username=username, sensor_category=sensor_category)

                    changeset['username_sha3'] = username_sha3
                    self.log.info('User opt-in profile changed: {changeset}', changeset=changeset)

                    # publish opt-in profile change event
                    topic = 'opt-in.{}.on-profile-change'.format(username_sha3)
                    await self.publish(topic, changeset, options=PublishOptions(acknowledge=True))
                    self.log.info('Published profile change set to "{topic}"',
                                  topic=vehicle.hl(topic, color='green'))

                    return changeset
                else:
                    self.log.info('User opt-in profile unchanged')

            uri = 'opt-in.{}.set-profile'.format(username_sha3)
            await self.register(set_profile, uri, RegisterOptions(details_arg='details'))
            self.log.info('Registered procedure "{proc}"', proc=vehicle.hl(uri, color='green'))

            # per-user WAMP procedure: opt-in.<sha3(username).set-profile
            #
            async def get_profile(username_sha3, details=None):
                """
                Get current user data sharing opt-in profile.

                :param username_sha3: Hashed username, in HEX (lowercase) encoding of SHA3-256(username)
                :type username_sha3: str

                :param details: WAMP call details.
                :type details: :class:`autobahn.wamp.types.CallDetails`

                :return: Current user profile (a dictionary with string keys and boolean values)
                :rtype: dict
                """
                assert type(username_sha3) == str
                assert details is None or isinstance(details, CallDetails)

                self.log.info('{method}(username_sha3={username_sha3}) [details]',
                              method=vehicle.hl('get_profile', color='green'),
                              username_sha3=vehicle.hl(username_sha3),
                              details=details)
                if username_sha3 not in self._usernames_sha3_map:
                    raise ApplicationError('road-vehicle.error.no-such-profile')

                username = self._usernames_sha3_map[username_sha3]

                assert username in self._vehicles
                vehicle = self._vehicles[username]

                return vehicle.get_profile()

            uri = 'opt-in.{}.get-profile'.format(username_sha3)
            await self.register(get_profile, uri, RegisterOptions(details_arg='details'))
            self.log.info('Registered procedure "{proc}"', proc=vehicle.hl(uri, color='green'))

    @inlineCallbacks
    def onLeave(self, details):
        self.log.info(vehicle.hl("VehicleClient.onLeave BEGIN: {}".format(details), color='white'))

        self._running = False

        for username, seller in self._sellers.items():
            try:
                self.log.info('Stopping seller for username {username} ..', username=vehicle.hl(username))
                yield seller.stop()
            except:
                self.log.failure()
        self._sellers = {}

        self.log.info(vehicle.hl('*' * 120))
        self.log.info('Operation summary: ran {iterations} iterations, {skipped} skipped loop iterations, encountered {errors} loop errors',
                      iterations=vehicle.hl(self._iterations, color='green'),
                      skipped=vehicle.hl(self._skipped, color='yellow'),
                      errors=vehicle.hl(self._errors, color='red'))
        self.log.info(vehicle.hl('*' * 120))

        if details.reason == 'wamp.close.normal':
            self.log.info('Shutting down ..')
            # user initiated leave => end the program
            try:
                self.config.runner.stop()
                self.disconnect()
            except:
                self.log.failure()
            from twisted.internet import reactor
            if reactor.running:
                reactor.stop()
        else:
            # continue running the program (let ApplicationRunner perform auto-reconnect attempts ..)
            self.log.info('Will continue to run (reconnect)!')

        self.log.info(vehicle.hl("VehicleClient.onLeave END", color='white'))


if __name__ == '__main__':

    vehicledata = vehicle.load_dataset('dataset1', 'samples.csv', vehicle.VehicleEvent.from_row_format2)
    for dataset in vehicledata:
        vehicles = vehicledata[dataset]
        print('data loaded from {} vehicles in dataset "{}"'.format(len(vehicles), dataset))

    # Crossbar.io connection configuration
    url = os.environ.get('CBURL', u'ws://localhost:8080/ws')
    realm = os.environ.get('CBREALM', u'realm1')
    privkey = os.environ.get('XBR_PRIVKEY', None)

    # print env vars of interest
    for e in os.environ:
        for prefix in ['CB', 'XBR', 'CFX']:
            if e.startswith(prefix):
                print('Environment {}={}'.format(e, os.environ[e]))

    # parse command line parameters
    parser = argparse.ArgumentParser()

    parser.add_argument('-d',
                        '--debug',
                        action='store_true',
                        help='Enable debug output.')

    parser.add_argument('--url',
                        dest='url',
                        type=six.text_type,
                        default=url,
                        help='The router URL (default: "ws://localhost:8080/ws").')

    parser.add_argument('--realm',
                        dest='realm',
                        type=six.text_type,
                        default=realm,
                        help='The realm to join (default: "realm1").')

    args = parser.parse_args()

    # start logging
    if args.debug:
        txaio.start_logging(level='debug')
    else:
        txaio.start_logging(level='info')

    # any extra info we want to forward to our ClientSession (in self.config.extra)
    extra = {
        'vehicledata': vehicledata,
    }

    # now actually run a WAMP client using our session class ClientSession
    runner = ApplicationRunner(url=args.url, realm=args.realm, extra=extra)
    runner.run(VehicleClient, auto_reconnect=True)
