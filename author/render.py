import math
from PIL import Image, ImageDraw
from figure import *

BG=(0x2B,0x2F,0x33); STAGE=(0x36,0x3B,0x40)
RED=(0xFF,0x28,0x00); RED_F=(0xA8,0x1A,0x00)
SKIN=(0xCF,0xD2,0xD6); SKIN_F=(0x8D,0x92,0x98)
SHORT=(0x0C,0x0C,0x0D); SHORT_F=(0x07,0x07,0x08)
SHOE=(0xF2,0xF2,0xF2); SHOE_F=(0xA9,0xAC,0xB0)
HAIR=(0x0C,0x0C,0x0D)
LINE=(0x5A,0x60,0x66); MAT=(0x4A,0x50,0x56); BARC=(0xBE,0xBE,0xBE)

def cap(d, a, b, w, col, S):
    d.line([(a[0]*S,a[1]*S),(b[0]*S,b[1]*S)], fill=col, width=max(1,int(w*S)))
    r = w*S/2
    for p in (a,b):
        d.ellipse([p[0]*S-r,p[1]*S-r,p[0]*S+r,p[1]*S+r], fill=col)

def lerp_pose(a, b, t):
    out = {}
    for k in a:
        if k in ("hip","pts"): continue
        va, vb = a[k], b[k]
        if isinstance(va,(int,float)) and isinstance(vb,(int,float)) and not isinstance(va,bool):
            out[k] = va + (vb-va)*t
        else: out[k]=va
    return out

def head_pts(C, h, hr, jaw, front):
    # local->world: forward axis f = right rotated by head angle, up axis u = up(h)
    ux, uy = up(h)
    fx, fy = -uy*-1, ux*-1  # placeholder, fixed below
    # forward is (cos h, sin h) rotated: for h=0 forward=(1,0), up=(0,-1). rotation by +h clockwise screen.
    r = math.radians(h)
    fx, fy = math.cos(r), math.sin(r)
    ux, uy = math.sin(r), -math.cos(r)
    def w(f, u): return (C[0] + (fx*f + ux*u)*hr, C[1] + (fy*f + uy*u)*hr)
    return w

def draw_fig(d, Q, S, sc=1.0, mirror=False, ox=0, front=False, colors=True):
    """Q solved & transformed (final world coords, hip in Q['hip']). sc scales sizes."""
    pts = Q["pts"]; hip = Q["hip"]
    def sv(*pp): return [(p[0], p[1]) for p in pp]
    wd = {k: v*sc for k, v in W.items()}
    # far limbs
    cap(d, pts["Sof"], pts["Ef"], wd["ua"], RED_F, S); cap(d, pts["Ef"], pts["Wf"], wd["fa"], SKIN_F, S); cap(d, pts["Wf"], pts["Hdf"], wd["fa"]*0.9, SKIN_F, S)
    # far leg
    cap(d, pts["Hof"], pts["Kf"], wd["th"], SHORT_F, S); cap(d, pts["Kf"], pts["Af"], wd["sh"], SKIN_F, S)
    cap(d, pts["Hlf"], pts["Tf"], wd["ft"], SHOE_F, S)
    # torso
    cap(d, pts["H"], pts["S"], wd["tor"], RED, S)
    # shorts block
    hb = add(pts["H"], up(Q["t"]), 0.055*sc)
    cap(d, pts["H"], hb, wd["short"], SHORT, S)
    # near leg
    cap(d, pts["Hon"], pts["Kn"], wd["th"], SHORT, S); cap(d, pts["Kn"], pts["An"], wd["sh"], SKIN, S)
    cap(d, pts["Hln"], pts["Tn"], wd["ft"], SHOE, S)
    # neck
    cap(d, pts["S"], pts["C"], 0.058*sc, SKIN, S)
    # head
    C = pts["C"]; hr = L["hr"]*sc
    w = head_pts(C, Q["h"], hr, Q["jaw"], front)
    def poly(pp, col): d.polygon([(x*S,y*S) for x,y in pp], fill=col)
    d.ellipse([(C[0]-hr)*S,(C[1]-hr)*S,(C[0]+hr)*S,(C[1]+hr)*S], fill=SKIN)
    if not front:
        ja = Q["jaw"]*math.radians(1)
        hinge=(-0.35,-0.25)
        base=[(-0.45,-0.2),(0.72,-0.30),(0.66,-0.92),(0.08,-1.12),(-0.45,-0.75)]
        ang = math.radians(Q["jaw"])
        rot=[]
        for f,u in base:
            df,du = f-hinge[0], u-hinge[1]
            # rotate chin downward/back: rotate clockwise in (f,u) frame => angle negative
            nf = hinge[0] + df*math.cos(ang) + du*math.sin(ang)
            nu = hinge[1] - df*math.sin(ang) + du*math.cos(ang)
            rot.append((nf,nu))
        poly([w(f,u) for f,u in rot], SKIN)
        # nose
        poly([w(0.92,0.10),w(1.26,-0.12),w(0.88,-0.30)], SKIN)
        # mouth gap when open
        if Q["jaw"]>3:
            poly([w(0.55,-0.32),w(0.95,-0.33),w(0.72+0.0,-0.33-0.22*math.sin(ang)*2.2)], BG)
        # eye
        e=w(0.50,0.20); d.ellipse([(e[0]-0.09*hr)*S,(e[1]-0.09*hr)*S,(e[0]+0.09*hr)*S,(e[1]+0.09*hr)*S], fill=HAIR)
        # hair cap
        arc=[w(1.03*math.cos(math.radians(a)),1.03*math.sin(math.radians(a))) for a in range(55,246,8)]
        poly(arc, HAIR)
    else:
        arc=[w(1.03*math.cos(math.radians(a)),1.03*math.sin(math.radians(a))) for a in range(5,176,8)]
        poly(arc, HAIR)
    # near arm
    cap(d, pts["Son"], pts["En"], wd["ua"], RED, S); cap(d, pts["En"], pts["Wn"], wd["fa"], SKIN, S); cap(d, pts["Wn"], pts["Hdn"], wd["fa"]*0.9, SKIN, S)

def draw_rope(d, Q, S, sc=1.0):
    if Q.get("rope") is None: return
    H = Q["pts"]["Hdn"]; R=0.50*sc
    a = Q["rope"]
    p1 = (H[0]+R*1.3*math.sin(math.radians(a+40)), H[1]-R*1.3*math.cos(math.radians(a+40)))
    p2 = (H[0]+R*1.3*math.sin(math.radians(a-40)), H[1]-R*1.3*math.cos(math.radians(a-40)))
    pp=[]
    for i in range(0,31):
        t=i/30; u=1-t
        x=u**3*H[0]+3*u*u*t*p1[0]+3*u*t*t*p2[0]+t**3*H[0]
        y=u**3*H[1]+3*u*u*t*p1[1]+3*u*t*t*p2[1]+t**3*H[1]
        pp.append((x*S,y*S))
    d.line(pp, fill=(0xF2,0xF2,0xF2), width=max(2,int(0.008*S)))
