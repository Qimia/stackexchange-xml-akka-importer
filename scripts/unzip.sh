#!/bin/sh
#Input target directory
echo "Please enter the path of zip files"
read ZIP_PATH

for zip in $ZIP_PATH/*.zip
do
  dirname=`echo $zip | sed 's/\.zip$//'`
  if mkdir "$dirname"
  then
    if cd "$dirname"
    then
      unzip "$zip"
      cd -
      # rm -f $zip # Uncomment to delete the original zip file
    else
      echo "Could not unpack $zip - cd failed"
    fi
  else
    echo "Could not unpack $zip - mkdir failed"
  fi
done
