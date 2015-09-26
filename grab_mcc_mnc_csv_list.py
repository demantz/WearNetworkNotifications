#!/usr/bin/python
# Get the Mobile Country Codes (MCC) and Mobile Network Codes (MNC) table
# from mcc-mnc.com and output it in CSV format.

# (c) by https://github.com/musalbas
# Modified (striped) by Dennis Mantz

import re
import urllib2

print "MCC,MNC,CC,Country,Network"

td_re = re.compile('<td>([^<]*)</td>'*6)

html = urllib2.urlopen('http://mcc-mnc.com/').read()

tbody_start = False
for line in html.split('\n'):
    if '<tbody>' in line:
        tbody_start = True
    elif '</tbody>' in line:
        break
    elif tbody_start:
        td_search = td_re.search(line)
        csv_line = ''
        for n in range(1, 7):
            if n == 5:
                continue
            group = td_search.group(n).strip().replace(',', '')
            csv_line += group
            if n != 6:
                csv_line += ','
        print csv_line
