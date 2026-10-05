import sys, os, json
import numpy as np
from PIL import Image, ImageDraw
A='/home/claude/grow/app/src/main/assets/anim'
meta=json.load(open(A+'/meta.json'))
def sheet(ids, path, frames=(0,2,4,6,8), tw=170, mirror=False):
    th=int(tw*1.25)
    rows=[]
    for i in ids:
        m=meta[i]; n=m['n']
        sel=[min(n-1,round(k*(n-1)/8)) for k in range(9)] if frames=='all' else [min(n-1,f) for f in frames]
        row=Image.new('RGB',(tw*len(sel)+4,th+14),(0,0,0))
        ImageDraw.Draw(row).text((3,1),f"{i}  {m['w']}x{m['h']}  n={n}",fill=(255,255,255))
        for c,k in enumerate(sel):
            st=Image.new('RGB',(tw-4,th-2),(190,192,198))
            im=Image.open(f'{A}/{i}/{k:02d}.webp').convert('RGBA')
            s=min((tw-12)/im.width,(th-12)/im.height); im=im.resize((int(im.width*s),int(im.height*s)),Image.LANCZOS)
            st.paste(im,((st.width-im.width)//2,(st.height-im.height)//2+4),im)
            row.paste(st,(c*tw+2,14))
        rows.append(row)
    S=Image.new('RGB',(rows[0].width,sum(r.height for r in rows)),(0,0,0)); y=0
    for r in rows: S.paste(r,(0,y)); y+=r.height
    S.save(path); return S.size
if __name__=='__main__':
    ids=sys.argv[1].split(','); print(sheet(ids,sys.argv[2], frames='all' if len(sys.argv)>3 and sys.argv[3]=='all' else (0,2,4,6,8)))
