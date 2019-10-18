import six
import binascii
import os
import argparse
from uuid import UUID

import txaio
txaio.use_twisted()

from autobahn.twisted.wamp import ApplicationSession, ApplicationRunner
from autobahn.twisted.util import sleep
from autobahn.twisted.xbr import SimpleSeller
from autobahn.wamp.serializer import CBORSerializer
from autobahn.wamp.types import PublishOptions


class Seller(ApplicationSession):

    async def onJoin(self, details):
        print('Seller session joined', details)

        market_maker_adr = binascii.a2b_hex(self.config.extra['market_maker_adr'][2:])
        print('Using market maker adr:', self.config.extra['market_maker_adr'])

        seller_privkey = binascii.a2b_hex(self.config.extra['seller_privkey'][2:])

        api_id = UUID('627f1b5c-58c2-43b1-8422-a34f7d3f5a04').bytes
        topic = 'com.example.location'
        counter = 1

        seller = SimpleSeller(market_maker_adr, seller_privkey)
        price = 35 * 10 ** 18
        interval = 10
        seller.add(api_id, topic, price, interval, None)

        balance = await seller.start(self)
        balance = int(balance / 10 ** 18)
        print("Remaining balance: {} XBR".format(balance))

        running = True
        samples = self.config.extra['samples']
        counter = 0

        while running:
            event = samples[counter]
            payload = event.marshal()
            key_id, enc_ser, ciphertext = await seller.wrap(api_id,
                                                            topic,
                                                            payload)

            pub = await self.publish(topic, key_id, enc_ser, ciphertext,
                                     options=PublishOptions(acknowledge=True))

            print('Published event {}: {}'.format(pub.id, payload))

            counter += 1
            await sleep(1)


if __name__ == '__main__':

    url = os.environ.get('XBR_INSTANCE', u'ws://localhost:8080/ws')
    realm = os.environ.get('XBR_REALM', u'realm1')
    market_maker_adr = os.environ.get('XBR_MARKET_MAKER_ADR', '0x3e5e9111ae8eb78fe1cc3bb8915d5d461f3ef9a9')
    seller_privkey = os.environ.get('XBR_SELLER_PRIVKEY', '0xadd53f9a7e588d003326d1cbf9e4a43c061aadd9bc938c843a79e7b4fd2ad743')
    serializers = [CBORSerializer()]

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

    vehicles = load_samples('../samples.csv')
    print('Ok, samples loaded for {} vehicle(s)'.format(len(vehicles)))

    # take vehicle event samples for first (alphabethical order) vehicle
    vehicle_samples = vehicles[sorted(vehicles.keys)[0]]
    print('Ok, will run with {} taken from first (alpha order) vehicle'.format(len(vehicle_samples)))

    # any extra info we want to forward to our ClientSession (in self.config.extra)
    extra = {
        'samples': vehicle_samples,
        'market_maker_adr': market_maker_adr,
        'seller_privkey': seller_privkey,
    }

    # now actually run a WAMP client using our session class ClientSession
    runner = ApplicationRunner(url=args.url, realm=args.realm, extra=extra, serializers=serializers)
    runner.run(Seller, auto_reconnect=True)
