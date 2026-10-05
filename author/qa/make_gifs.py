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
def render(i,t,w,h):
    m=meta[i]; fr=frames(i); e_loop=loops[i]
    base=stage(w,h).convert('RGBA')
    loop=e_loop*2 if m['alt'] else e_loop
    # layout identical to FigureView.layout()
    if m['bust']:
        s=min(w*0.98/m['w'],h/m['h']); dw,dh=m['w']*s,m['h']*s; x0=(w-dw)/2; y0=h-dh
    else:
        s=min(w*0.92/m['w'],h*0.88/m['h']); dw,dh=m['w']*s,m['h']*s
        wide=m['w']/m['h']>1.1
        cy=h*0.58 if wide else (h*0.50 if not m['ground'] else h*0.94-dh/2)
        x0=(w-dw)/2; y0=cy-dh/2
    def blit(img,flip,alpha):
        im=img.resize((max(1,int(dw)),max(1,int(dh))),Image.LANCZOS)
        if flip: im=im.transpose(Image.FLIP_LEFT_RIGHT)
        a=np.asarray(im).copy().astype(np.float32); a[...,3]*=alpha
        layer=Image.new('RGBA',(w,h),(0,0,0,0)); layer.paste(Image.fromarray(a.astype(np.uint8),'RGBA'),(int(x0),int(y0)))
        base.alpha_composite(layer)
    if not m['step']:
        ph=(t/loop)%1.0; fi=smoothpos(ph)*(m['n']-1); a=int(fi); b=min(a+1,m['n']-1); f=fi-a
        blit(fr[a],False,1-f if b!=a else 1.0)
        if b!=a and f>0.01: blit(fr[b],False,f)
    else:
        bs=beats(m['n'],m['alt']); L=len(bs); fl=[m['alt'] and k>=L//2 for k in range(L)]
        per=max(loop/L,0.16); u=(t/per)%L; k=int(u)%L; f=u-int(u); fade=0.30
        tb=smooth((f-(1-fade))/fade) if f>1-fade else 0.0; nx=(k+1)%L
        blit(fr[bs[k]],fl[k],1-tb)
        if tb>0.01: blit(fr[bs[nx]],fl[nx],tb)
    return base.convert('RGB')
def gif(ids,path,cols=4,tw=150,th=190,fps=12,secs=5):
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
