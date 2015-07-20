#!/bin/bash

array_contains () {
    local array="$1[@]"
    local seeking=$2
    local in=1
    for element in "${!array}"; do
        if [[ $element == $seeking ]]; then
            in=0
            break
        fi
    done
    return $in
}

excludes=("blowpipe.g3db"\
          "box.g3db"\
          "mask.g3db"\
          "skybox.g3db"\
          "shard.g3db"\
          "gun.g3db"\
          )

for i in *.g3db; do
    array_contains excludes $i && echo "keeping '"$i"'" || rm -v $i
done

