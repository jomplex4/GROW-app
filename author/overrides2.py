from build import *
import build
from overrides1 import override

# ---------------- FLOOR EXERCISES (head to the right, mat on floor) ----------------
# Knees to chest: lying on back, knees hugged toward chest (toward the head, right side), feet near buttocks
override("knees2chest", poses=[
    P(t=90, h=90, fo=0, tn=-90, sn=-90, tf=-90, sf=-90, un=-90, fn=-90, uf=-90, ff=-90),
    P(t=90, h=84, fo=70, tn=128, sn=-62, tf=134, sf=-66, ra=(0.12, -0.12, -1), la=(0.12, -0.12, -1)),
    P(t=90, h=86, fo=70, tn=146, sn=-52, tf=150, sf=-56, ra=(0.10, -0.16, -1), la=(0.10, -0.16, -1)),
], loop=5.0, ease=1, mat=True, hold=True)

# Full body stretch: lying on back, arms resting overhead on the mat, legs long, reach both ways
override("elongate", poses=[
    P(t=90, h=90, fo=0, tn=-90, sn=-90, tf=-90, sf=-90, un=-82, fn=-84, uf=-82, ff=-84),
    P(t=90, h=90, fo=-20, tn=-90, sn=-90, tf=-90, sf=-90, ra=(0.64, 0.0, -1), la=(0.64, 0.0, -1)),
    P(t=90, h=90, fo=-20, tn=-90, sn=-90, tf=-90, sf=-90, ra=(0.66, 0.0, -1), la=(0.66, 0.0, -1)),
], loop=5.0, ease=1, mat=True, hold=True)

# Glute bridge: shoulders and head stay on the floor, hips rise, feet flat, knees pointing up
override("bridge", poses=[
    P(t=90, h=90, un=-90, fn=-90, uf=-90, ff=-90, fo=90, rl=(-0.17, 0.02, -1), ll=(-0.17, 0.02, -1)),
    P(t=114, h=94, un=-94, fn=-94, uf=-94, ff=-94, fo=90, rl=(-0.20, 0.14, -1), ll=(-0.20, 0.14, -1)),
], loop=3.6, ease=1, mat=True)

# Child pose: kneeling, sitting back on the heels, arms stretched forward on the mat
override("child", poses=[
    P(t=70, h=84, tn=64, sn=-92, tf=64, sf=-92, fo=0, ra=(0.52, 0.12, -1), la=(0.52, 0.12, -1)),
    P(t=78, h=90, tn=62, sn=-92, tf=62, sf=-92, fo=0, ra=(0.56, 0.12, -1), la=(0.56, 0.12, -1)),
], loop=4.5, ease=1, mat=True, hold=True)

# Dead bug: on back, arms up, knees bent 90 over hips; extend opposite arm and leg
override("deadbug", poses=[
    P(t=90, h=90, un=180, fn=180, uf=180, ff=180, tn=170, sn=-80, tf=170, sf=-80, fo=60, fof=60),
    P(t=90, h=90, un=180, fn=180, uf=95, ff=95, tn=170, sn=-80, tf=-92, sf=-92, fo=60, fof=0),
    P(t=90, h=90, un=180, fn=180, uf=180, ff=180, tn=170, sn=-80, tf=170, sf=-80, fo=60, fof=60),
    P(t=90, h=90, un=95, fn=95, uf=180, ff=180, tn=-92, sn=-92, tf=170, sf=-80, fo=0, fof=60),
], loop=4.5, ease=1, mat=True, lvl=2)

# Chest opener: stand tall, hands clasped behind the back, lift and open the chest
override("chestopen", anchor="hip", poses=[
    P(t=0, un=-6, fn=-6, uf=-6, ff=-6),
    P(t=-4, h=-8, un=-52, fn=-52, uf=-56, ff=-56),
    P(t=-4, h=-8, un=-52, fn=-52, uf=-56, ff=-56),
], loop=5.0, ease=1, hold=True)

# Overhead reach: front view, rise on toes, reach tall
override("reach", front=True, anchor="hip", poses=[
    P(t=0, un=10, fn=10, uf=-10, ff=-10, tn=1, tf=-1),
    P(t=0, un=172, fn=174, uf=-172, ff=-174, tn=1, tf=-1, fo=60, fof=60, lift=0.0),
], loop=3.4, ease=1)
