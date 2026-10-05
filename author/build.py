import math, json, sys
from PIL import Image, ImageDraw, ImageFont
from figure import *
from render import *

EX = []   # list of exercise dicts

def P(**k): return k

def E(id, name, pillar, met, desc, tips, poses, loop=1.0, ease=1, sided=False, lvl=1, anchor="hip", ax=0.5,
      bar=False, wall=None, mat=False, front=False, bust=False, smax=1.5, rope=False, hold=False):
    EX.append(dict(id=id,name=name,pillar=pillar,met=met,desc=desc,tips=tips,poses=poses,loop=loop,ease=ease,sided=sided,
                   lvl=lvl,anchor=anchor,ax=ax,bar=bar,wall=wall,mat=mat,front=front,bust=bust,smax=smax,rope=rope,hold=hold))

def prepare(e):
    solved = []
    for p in e["poses"]:
        q = dict(p)
        if e["front"]: q["front"]=True
        a = e["anchor"]
        bar = None
        solved.append(solve(q, anchor=a, ax=e["ax"], ground=not e["bust"]))
    # bar prop y (raw): at grab y (hands wrist) minus small
    props = {}
    # bbox
    xs=[];ys=[]
    for Q in solved:
        for x,y,r in extent(Q, Q["pts"]):
            xs += [x-r,x+r]; ys += [y-r,y+r]
    if e["bar"]:
        gy = solved[0]["grab"]
        props["bar"] = gy - 0.012
        ys.append(gy-0.03)
    if e["wall"] is not None:
        props["wall"] = solved[0]["hip"][0] + e["wall"]
        xs.append(props["wall"]+0.02)
    if e["mat"]:
        xs += [min(xs)-0.04, max(xs)+0.04]
    x0,x1,y0,y1 = min(xs),max(xs),min(ys),max(ys)
    if e["bust"]:
        C0 = solved[0]["pts"]["C"]
        s = e["smax"]
        T = lambda x,y: ((x-C0[0])*s+0.5, (y-C0[1])*s+0.43)
    else:
        bw = x1-x0
        if e["bar"]:
            bh = y1 - y0; s = min(0.90/bw, 0.76/bh, e["smax"]); cx=(x0+x1)/2
            T = lambda x,y: ((x-cx)*s+0.5, (y-y1)*s+0.84)
        else:
            bh = FLOOR - y0
            s = min(0.94/bw, 0.88/bh, e["smax"])
            cx = (x0+x1)/2
            T = lambda x,y: ((x-cx)*s+0.5, (y-FLOOR)*s+0.90)
    out = []
    for Q in solved:
        Z = dict(Q); Z["hip"] = T(*Q["hip"]); Z["pts"]={k:T(*v) for k,v in Q["pts"].items()}
        out.append(Z)
    pr = {}
    if "bar" in props: pr["bar"] = T(0,props["bar"])[1]
    if "wall" in props: pr["wall"] = T(props["wall"],0)[0]
    if e["mat"]:
        pr["mat"] = [T(x0-0.04,0)[0], T(x1+0.04,0)[0]]
    return out, s, pr

def draw_scene(Q, s, pr, e, W=360, H=420, S=2):
    img = Image.new("RGB",(W*S,H*S),STAGE); d = ImageDraw.Draw(img)
    u = min(W,H)
    # world square centered
    ox = (W-u)/2; oy=(H-u)/2
    def tf(p): return (ox/ u + p[0], oy/u + p[1])
    # rescale: draw in normalized coordinates multiplied by (u*S); offset handled by translation
    sc = u*S
    layer = Image.new("RGB",(W*S,H*S),STAGE); dd = ImageDraw.Draw(layer)
    # floor
    fy = (0.90*u+oy)*S
    if not e["bust"]:
        dd.line([(0,fy),(W*S,fy)], fill=LINE, width=int(0.006*sc))
        if "mat" in pr:
            m=pr["mat"]; dd.rounded_rectangle([(m[0]*u+ox)*S,fy-0.004*sc,(m[1]*u+ox)*S,fy+0.010*sc], radius=0.006*sc, fill=RED)
        if "bar" in pr:
            by=(pr["bar"]*u+oy)*S
            dd.line([(0.06*W*S,by),(0.94*W*S,by)], fill=BARC, width=int(0.014*sc))
        if "wall" in pr:
            wx=(pr["wall"]*u+ox)*S
            dd.rectangle([wx,0.05*H*S,wx+0.03*sc,fy], fill=MAT)
    # build scaled coords: shift pts to canvas normalized
    Z = dict(Q); 
    def shp(p): return (p[0]*u+ox, p[1]*u+oy)
    Z["pts"]={k:shp(v) for k,v in Q["pts"].items()}; Z["hip"]=shp(Q["hip"])
    # draw_fig expects world*S scale: pass S and scale sizes by u
    draw_fig_px(dd, Z, S, u, s)
    return layer

def draw_fig_px(d, Q, S, u, s):
    # emulate draw_fig with world->pixel scale u
    import render as R
    pts = Q["pts"]
    # build a world-normalized copy
    Z = dict(Q); Z["pts"]={k:(v[0]/u, v[1]/u) for k,v in pts.items()}; Z["hip"]=(Q["hip"][0]/u,Q["hip"][1]/u)
    # draw into pixel space by scaling S
    R.draw_fig(d, Z, S*u, sc=s, front=Q.get("front",False))
    if Q.get("rope") is not None:
        R.draw_rope(d, Z, S*u, s)

def keyframe_strip(e, out, s, pr, W=240, H=290):
    n = len(out)
    img = Image.new("RGB",(W*n, H+26),BG)
    for i,Q in enumerate(out):
        lay = draw_scene(Q, s, pr, e, W, H, 2).resize((W,H), Image.LANCZOS)
        img.paste(lay,(i*W,26))
    ImageDraw.Draw(img).text((6,6), f'{e["id"]}  {e["name"]}  scale={s:.2f}', fill=(255,255,255))
    return img

def preview(ids=None, path="sheet.png"):
    strips=[]
    for e in EX:
        if ids and e["id"] not in ids: continue
        out,s,pr = prepare(e)
        strips.append(keyframe_strip(e,out,s,pr))
    if not strips: return
    Wd = max(i.width for i in strips); Ht = sum(i.height for i in strips)
    sheet = Image.new("RGB",(Wd,Ht),BG); y=0
    for i in strips: sheet.paste(i,(0,y)); y+=i.height
    sheet.save(path)
