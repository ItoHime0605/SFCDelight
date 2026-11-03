"""Portable PNG sprite validation: Minecraft-compatible 8-bit RGBA and indexed PNG."""
from pathlib import Path
import struct
import zlib

def validate_icon(path, expected_size=32):
    path=Path(path)
    data=path.read_bytes()
    assert data[:8]==bytes((137,80,78,71,13,10,26,10)),path
    chunks={};p=8
    while p<len(data):
        n=struct.unpack_from(">I",data,p)[0]
        typ=data[p+4:p+8]
        payload=data[p+8:p+8+n]
        assert zlib.crc32(typ+payload)==struct.unpack_from(">I",data,p+8+n)[0],(path,typ)
        chunks[typ]=chunks.get(typ,b"")+payload
        p+=n+12
        if typ==b"IEND":break
    assert p==len(data),path
    width,height,depth,colour,comp,filtr,interlace=struct.unpack(">IIBBBBB",chunks[b"IHDR"])
    assert (width,height,depth,comp,filtr,interlace)==(expected_size,expected_size,8,0,0,0),path
    assert colour in (3,6),(path,colour)
    stride=expected_size*(1 if colour==3 else 4)
    bpp=1 if colour==3 else 4
    raw=zlib.decompress(chunks[b"IDAT"])
    assert len(raw)==expected_size*(1+stride),path
    if colour==3:
        palette=chunks[b"PLTE"]
        transparency=chunks.get(b"tRNS",b"")
        assert len(palette)%3==0
        assert len(palette)>=30 and transparency,(path,"missing palette transparency")
    previous=bytearray(stride);coverage=0;clear=0;colours=set()
    for y in range(expected_size):
        filt=raw[y*(stride+1)]
        assert 0<=filt<=4,(path,y)
        row=bytearray(raw[y*(stride+1)+1:(y+1)*(stride+1)])
        for k in range(stride):
            left=row[k-bpp] if k>=bpp else 0
            up=previous[k]
            upper_left=previous[k-bpp] if k>=bpp else 0
            if filt==0:predict=0
            elif filt==1:predict=left
            elif filt==2:predict=up
            elif filt==3:predict=(left+up)//2
            else:
                z=left+up-upper_left
                da,db,dc=abs(z-left),abs(z-up),abs(z-upper_left)
                predict=left if da<=db and da<=dc else up if db<=dc else upper_left
            row[k]=(row[k]+predict)&255
        for x in range(expected_size):
            if colour==6:
                pix=tuple(row[x*4:x*4+4])
                red,green,blue,alpha=pix
            else:
                index=row[x]
                assert 3*index+2<len(palette),(path,y,x,index)
                red,green,blue=palette[index*3:index*3+3]
                alpha=transparency[index] if index<len(transparency) else 255
            if alpha>0:
                coverage+=1;colours.add((red,green,blue))
            else:clear+=1
        previous=row
    assert 100 * (expected_size//32)**2 <= coverage <= 830 * (expected_size//32)**2,(path,"coverage",coverage)
    assert clear>=150 * (expected_size//32)**2,(path,"transparent pixels",clear)
    assert len(colours)>=10,(path,"colour variety",len(colours))
    return coverage
