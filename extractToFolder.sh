#!/bin/bash

if [ $# -ne 2 ]; then
    echo Usage: ./extractToFolder [folderWithTars] [destination]
    exit 1
fi

#overwrite things in destination
#rm -r $2

# if [!(-d $2)]; then
# 	mkdir $2
# fi

mkdir -p $2
mkdir temp

for file in $(ls $1 | grep tgz); do
	
	echo processing $file...
	fileName=${file:0:8}

	tar -xvzf $1/$file -C ./temp

	mkdir -p $2/$fileName
	mv temp/* $2/$fileName

done

rm -r temp

echo Extraction complete