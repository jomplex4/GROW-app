import math, json
from PIL import Image, ImageDraw

FLOOR = 0.90
# segment lengths (world units)
L = dict(tor=0.30, neck=0.03, hr=0.072, ua=0.16, fa=0.15, hand=0.035, th=0.24, sh=0.23, ft=0.085)
W = dict(tor=0.118, short=0.124, th=0.080, sh=0.056, ua=0.058, fa=0.046, ft=0.046)
def dn(a):
    r = math.radians(a); return (math.sin(r), math.cos(r))
def up(a):
    r = math.radians(a); return (math.sin(r), -math.cos(r))
def add(p, v, k=1.0): return (p[0]+v[0]*k, p[1]+v[1]*k)

KEYS = ["t","h","hf","un","fn","uf","ff","tn","sn","tf","sf","jaw","fo","fof","kn","kf","cn","cf"]

def fk(P, hip=(0.0,0.0)):
    """P: dict of angles. returns dict of joint points"""
    H = hip
    S = add(H, up(P["t"]), L["tor"])
    C = add(add(S, up(P["h"]), L["neck"]+L["hr"]), (P["hf"],0))
    pts = dict(H=H, S=S, C=C)
    for side, (u, f, t, s) in (("n",(P["un"],P["fn"],P["tn"],P["sn"])), ("f",(P["uf"],P["ff"],P["tf"],P["sf"]))):
        sg = 1 if side=="n" else -1
        fr = P.get("front", False)
        So = add(S, (sg*0.07,0)) if fr else S
        Ho = add(H, (sg*0.04,0)) if fr else H
        E = add(So, dn(u), L["ua"]); Wr = add(E, dn(f), L["fa"]); Hd = add(Wr, dn(f), L["hand"])
        kk = P.get("kn",1) if side=="n" else P.get("kf",1); cc = P.get("cn",1) if side=="n" else P.get("cf",1)
        K = add(Ho, dn(t), L["th"]*kk); A = add(K, dn(s), L["sh"]*cc)
        fo = P.get("fo",90) if side=="n" else P.get("fof",P.get("fo",90)); fd = dn(s + (fo if (side=="n" or not P.get("front")) else -fo))
        T = add(A, fd, L["ft"]); Hl = add(A, fd, -0.02)
        pts.update({"So"+side:So,"Ho"+side:Ho,"E"+side:E, "W"+side:Wr, "Hd"+side:Hd, "K"+side:K, "A"+side:A, "T"+side:T, "Hl"+side:Hl})
    return pts

def extent(P, pts):
    """list of (x,y,r) drawn points for bbox/contact (radii match the renderer)"""
    out = []
    for k, rad in (("H",0.066),("S",0.052),("C",L["hr"]),("En",.030),("Ef",.030),("Wn",.022),("Wf",.022),("Hdn",.030),("Hdf",.030),
                   ("Kn",.042),("Kf",.042),("An",.030),("Af",.030),("Tn",.030),("Tf",.030),("Hln",.030),("Hlf",.030)):
        out.append((pts[k][0], pts[k][1], rad))
    return out

def default(P):
    Q = dict(t=0,h=None,hf=0,un=0,fn=None,uf=None,ff=None,tn=0,sn=None,tf=None,sf=None,jaw=0,fo=90,fof=None,kn=1,kf=1,cn=1,cf=1,rope=None,lift=0,grab=None,front=False,ra=None,la=None,rl=None,ll=None)
    Q.update(P)
    if Q["h"] is None: Q["h"]=Q["t"]
    if Q["fof"] is None: Q["fof"]=Q["fo"]
    if Q["fn"] is None: Q["fn"]=Q["un"]
    if Q["uf"] is None: Q["uf"]=Q["un"]
    if Q["ff"] is None: Q["ff"]=Q["fn"]
    if Q["sn"] is None: Q["sn"]=Q["tn"]
    if Q["tf"] is None: Q["tf"]=Q["tn"]
    if Q["sf"] is None: Q["sf"]=Q["sn"]
    # IK
    Sx = L["tor"]*math.sin(math.radians(Q["t"])); Sy = -L["tor"]*math.cos(math.radians(Q["t"]))
    for key,(ku,kf) in (("ra",("un","fn")),("la",("uf","ff"))):
        if Q[key] is not None:
            dx,dy,bend = Q[key]
            Q[ku],Q[kf] = ik2((dx-Sx,dy-Sy), L["ua"], L["fa"], bend)
    for key,(kt,ks) in (("rl",("tn","sn")),("ll",("tf","sf"))):
        if Q[key] is not None:
            dx,dy,bend = Q[key]
            Q[kt],Q[ks] = ik2((dx,dy), L["th"], L["sh"], bend)
    return Q

def ik2(d, l1, l2, bend):
    dist = math.hypot(d[0], d[1])
    dist = max(abs(l1-l2)+1e-4, min(l1+l2-1e-4, dist))
    phi = math.degrees(math.atan2(d[0], d[1]))
    a = math.degrees(math.acos(max(-1,min(1,(l1*l1+dist*dist-l2*l2)/(2*l1*dist)))))
    u = phi + bend*a
    E = (l1*math.sin(math.radians(u)), l1*math.cos(math.radians(u)))
    f = math.degrees(math.atan2(d[0]-E[0], d[1]-E[1]))
    return u, f

def solve(P, anchor="hip", ax=0.5, bar=None, ground=True):
    Q = default(P)
    pts = fk(Q)
    ext = extent(Q, pts)
    low = max(y+r for x,y,r in ext)
    if Q["grab"] is not None:
        hy = Q["grab"] - pts["Hdn"][1]
    elif ground:
        hy = FLOOR - low
    else:
        hy = 0.5
    hy -= Q["lift"]
    if anchor=="hip": hx = ax
    elif anchor=="foot": hx = ax - pts["An"][0]
    elif anchor=="hand": hx = ax - pts["Wn"][0]
    elif anchor=="head": hx = ax - pts["C"][0]
    else: hx = ax
    Q["hip"]=(hx,hy)
    Q["pts"]=fk(Q,(hx,hy))
    return Q
