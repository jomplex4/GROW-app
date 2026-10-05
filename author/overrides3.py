from build import *
from overrides1 import override

# Quad stretch: stand on the near leg, hold the far foot at the glute with the near hand
override("quadstretch", anchor="hip", poses=[
    P(t=0, un=-4, fn=-4, uf=12, ff=12, tn=0, sn=0, tf=0, sf=0),
    P(t=-2, uf=-6, ff=-6, un=10, fn=10, tf=0, sf=0, tn=-6, sn=-168, la=(-0.07, 0.02, -1)),
    P(t=-2, uf=-6, ff=-6, un=10, fn=10, tf=0, sf=0, tn=-6, sn=-168, la=(-0.07, 0.02, -1)),
], loop=5.0, sided=True, hold=True, ease=1)

override("jj", front=True, anchor="hip", poses=[
    P(t=0, un=14, fn=16, uf=-14, ff=-16, tn=3, tf=-3),
    P(t=0, un=166, fn=170, uf=-166, ff=-170, tn=24, tf=-24, lift=0.05)], loop=0.9)
