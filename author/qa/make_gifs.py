import json, math, sys, os
import numpy as np
from PIL import Image
A='/home/claude/grow/app/src/main/assets'
ex=json.load(open(A+'/exercises.json')); meta=json.load(open(A+'/anim/meta.json'))
loops={e['id']:e['lp'] for e in ex}
def beats(n,alt):
    one=list(range(n))+list(range(n-2,0,-1))
    if alt: one=one+one
    return one
def smooth(x): x=min(1,max(0,x)); return x*x*(3-2*x)
def smoothpos(ph):
    p0=0.5-0.5*math.cos(2*math.pi*ph); d=0.10; x=min(1,max(0,(p0-d)/(1-2*d))); return x*x*(3-2*x)
FR={}
def frames(i):
    if i not in FR:
        m=meta[i]; FR[i]=[Image.open(f'{A}/anim/{i}/{k:02d}.webp').convert('RGBA') for k in range(m['n'])]
    return FR[i]
def stage(w,h):
    g=np.zeros((h,w,3),np.uint8)
    for y in range(h):
        t=y/(h-1); g[y,:]=[int(0xCF+(0xB4-0xCF)*t),int(0xD0+(0xB6-0xD0)*t),int(0xD5+(0xBD-0xD5)*t)]
    return Image.fromarray(g,'RGB')
SIDED={e['id']:e['sd'] for e in ex}
def profile(p,ri,ho,fa):
    p=p%1.0
    if p<ri: return smooth(p/ri) if ri>0 else 1.0
    if p<ri+ho: return 1.0
    if p<ri+ho+fa: return 1.0-smooth((p-ri-ho)/fa) if fa>0 else 0.0
    return 0.0
def profile(p,ri,ho,fa):
    p=p%1.0
    if p<ri: return smooth(p/ri) if ri>0 else 1.0
    if p<ri+ho: return 1.0
    if p<ri+ho+fa: return 1.0-smooth((p-ri-ho)/fa) if fa>0 else 0.0
    return 0.0
SIDED={e['id']:e['sd'] for e in ex}
def render(i,t,w,h):
    """same playback as FigureView.drawSprite"""
    m=meta[i]; fr=frames(i); n=m['n']
    opaque=m.get('opaque',0)
    base=(Image.new('RGBA',(w,h),(0xD1,0xD2,0xD7,255)) if opaque else stage(w,h).convert('RGBA'))
    per=m.get('loop') or loops[i]
    mirror=False; vis=1.0
    if SIDED[i]:
        mirror=int(t/per)%2==1
        d=min((t/per)%1.0,1-(t/per)%1.0)*per; vis*=smooth(d/0.22) if not opaque else 1.0
    if opaque:
        s=min(w/m['w'],h/m['h']); dw,dh=m['w']*s,m['h']*s; x0=(w-dw)/2; y0=(h-dh)/2
    elif m['bust']:
        s=min(w*0.98/m['w'],h/m['h']); dw,dh=m['w']*s,m['h']*s; x0=(w-dw)/2; y0=h-dh
    else:
        s=min(w*0.92/m['w'],h*0.88/m['h']); dw,dh=m['w']*s,m['h']*s
        wide=m['w']/m['h']>1.1; cy=h*0.58 if wide else (h*0.50 if not m['ground'] else h*0.94-dh/2)
        x0=(w-dw)/2; y0=cy-dh/2
    def blit(img,fl,alpha,dy=0.0,sy=1.0):
        im=img.resize((max(1,int(dw)),max(1,int(dh*sy))),Image.LANCZOS)
        if fl: im=im.transpose(Image.FLIP_LEFT_RIGHT)
        im=im.convert('RGBA'); a=np.asarray(im).copy().astype(np.float32); a[...,3]*=alpha
        layer=Image.new('RGBA',(w,h),(0,0,0,0))
        layer.paste(Image.fromarray(a.astype(np.uint8),'RGBA'),(int(x0),int(y0+dh-dh*sy+dy)))
        if mirror: layer=layer.transpose(Image.FLIP_LEFT_RIGHT)
        base.alpha_composite(layer)
    if opaque and n==1:
        idle=m.get('idle','breath'); loop=per
        if idle=='bounce':
            hop=abs(math.sin(math.pi*t/loop)); blit(fr[0],False,1.0,dy=-hop*0.05*dh)
        elif idle=='alt':
            beat=t/loop; idx=int(beat); f=beat-idx; flipA=idx%2==1
            fade=smooth((f-0.78)/0.22) if f>0.78 else 0.0
            bob=-0.02*dh*abs(math.sin(math.pi*f)) if loop<2 else 0.0
            blit(fr[0],flipA,1-fade,dy=bob)
            if fade>0.01: blit(fr[0],not flipA,fade,dy=bob)
        elif idle=='run':
            ph=(t/loop)%1.0; bob=-(0.5-0.5*math.cos(2*math.pi*ph))*0.035*dh; blit(fr[0],False,1.0,dy=bob)
        else:
            k=1+0.012*math.sin(2*math.pi*t/4.2); blit(fr[0],False,1.0,sy=k)
        return base.convert('RGB')
    total=per*2 if m['alt'] else per
    ph=(t/total)%1.0; flip=False; local=ph
    if m['alt']: flip=ph>=0.5; local=(ph*2)%1.0
    if m['alt']==2 and not opaque:
        d=min(local,1-local)*per; vis*=smooth(d/0.22)
    if m.get('mode')=='cy':
        fi=local*n; a=int(fi)%n; b=(a+1)%n; f=fi-int(fi)
        if n<=6: f=smooth(min(1,max(0,(f-0.72)/0.28)))
    else:
        pos=profile(local,m.get('ri',.5),m.get('ho',0),m.get('fa',.5)); fi=pos*(n-1); a=int(fi); b=min(a+1,n-1); f=fi-a
    blit(fr[a],flip,(1-f if b!=a else 1.0)*vis)
    if b!=a and f>0.01: blit(fr[b],flip,f*vis)
    return base.convert('RGB')
def gif(ids,path,cols=4,tw=160,th=200,fps=10,secs=12):
    rows=(len(ids)+cols-1)//cols; frames_out=[]
    for k in range(fps*secs):
        t=k/fps; sheet=Image.new('RGB',(tw*cols,th*rows),(0,0,0))
        for n,i in enumerate(ids): sheet.paste(render(i,t,tw-4,th-4),((n%cols)*tw+2,(n//cols)*th+2))
        frames_out.append(sheet.quantize(colors=96,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE))
    frames_out[0].save(path,save_all=True,append_images=frames_out[1:],duration=int(1000/fps),loop=0,optimize=True)
    print(path,os.path.getsize(path)//1024,'KB')
if __name__=='__main__':
    ids=[e['id'] for e in ex]
    main=[i for i in ids if not meta[i]['bust']]; jaw=[i for i in ids if meta[i]['bust']]
    groups=[main[0:9],main[9:18],main[18:27],main[27:36],jaw]
    names=['GROW_anim_1_calentamiento_impacto_carrera.gif','GROW_anim_2_colgado_suelo.gif','GROW_anim_3_core_suelo.gif','GROW_anim_4_flexibilidad.gif','GROW_anim_5_mandibula_cuello.gif']
    sel=int(sys.argv[1]) if len(sys.argv)>1 else None
    for g,nm in zip(groups,names):
        if sel is not None and names.index(nm)!=sel: continue
        gif(g,'/tmp/'+nm,cols=3 if len(g)<=9 else 4)
