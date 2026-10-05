
from build import *
import ex1, ex2, ex3
import build

def override(id, **kw):
    for e in build.EX:
        if e["id"] == id:
            e.update(kw); return
    raise KeyError(id)

# ---------------- FRONT-VIEW CARDIO ----------------
S0 = dict(un=12, fn=14, uf=-12, ff=-14)
override("rope", front=True, anchor="hip", smax=1.25, poses=[
    P(t=0, un=14, fn=72, uf=-14, ff=-72, tn=1, tf=-1, rope=0),
    P(t=0, un=14, fn=72, uf=-14, ff=-72, tn=2, tf=-2, cn=0.92, cf=0.92, rope=90, lift=0.035),
    P(t=0, un=14, fn=72, uf=-14, ff=-72, tn=2, tf=-2, cn=0.85, cf=0.85, rope=180, lift=0.06),
    P(t=0, un=14, fn=72, uf=-14, ff=-72, tn=1, tf=-1, cn=0.95, cf=0.95, rope=270, lift=0.02)], loop=0.8)

override("highknee", front=True, anchor="hip", poses=[
    P(t=2, un=10, fn=172, uf=-14, ff=-6, tn=3, kn=0.30, cn=1, sn=0, tf=-2, lift=0.03),
    P(t=2, un=14, fn=6, uf=-10, ff=-172, tn=2, tf=-3, kf=0.30, sf=0, cf=1, lift=0.03)], loop=0.55)

override("buttkick", front=True, anchor="hip", poses=[
    P(t=2, un=10, fn=150, uf=-14, ff=-6, tn=3, sn=3, cn=0.32, tf=-2, sf=-2, lift=0.02),
    P(t=2, un=14, fn=6, uf=-10, ff=-150, tn=2, sn=2, tf=-3, sf=-3, cf=0.32, lift=0.02)], loop=0.7)

override("march", front=True, anchor="hip", poses=[
    P(t=2, un=10, fn=130, uf=-14, ff=-20, tn=3, kn=0.55, sn=0, tf=-2, sf=-2),
    P(t=2, un=14, fn=20, uf=-10, ff=-130, tn=2, tf=-3, kf=0.55, sf=0)], loop=1.0)

override("squatjump", front=True, anchor="hip", smax=1.2, poses=[
    P(t=4, un=-18, fn=-8, uf=18, ff=8, tn=40, sn=-6, tf=-40, sf=6),
    P(t=0, un=170, fn=172, uf=-170, ff=-172, tn=2, sn=2, tf=-2, sf=-2, lift=0.12),
    P(t=3, un=8, fn=4, uf=-8, ff=-4, tn=24, sn=-4, tf=-24, sf=4)], loop=1.5)
