#!/bin/bash

for i in *.obj; do
    fbx-conv ${i}
done

rm *.obj *.mtl
