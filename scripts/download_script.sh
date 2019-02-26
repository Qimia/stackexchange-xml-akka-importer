#!/bin/bash
CONFIGFILE=../qimia_xmltocsv_config.ini
# Check config file
if [ ! -f $CONFIGFILE ]; then
   echo "Make sure $CONFIGFILE is in the base directory"
   exit 1
fi
# Get and check data directory
dat_dir=$(awk -F "=" '/data_folder/ {print $2}' $CONFIGFILE | tr -d ' ')
if [ ! -d $dat_dir ]; then
   echo "Make sure the data folder: $dat_dir exists"
   exit 1
fi
# path of torrent link"
TORRENT_LINK=https://archive.org/download/stackexchange/stackexchange_archive.torrent
echo $dat_dir
TARGET_DIR="$dat_dir/torrent_files"
# Create directory if it does not exist
mkdir -p "$TARGET_DIR"

if [ $(dpkg-query -W -f='${Status}' transmission-cli>/dev/null | grep -c "ok installed") -eq 0 ];
then
  apt-get install transmission-cli;
fi

#check weather torrent link exist or not
if $(wget --spider "$TORRENT_LINK" >/dev/null);
 then
	transmission-cli -w "$TARGET_DIR" "$TORRENT_LINK";
else
  echo "File does not exist"
	exit 1
fi


