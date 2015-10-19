PAT='^\(.*<b>Twidlit \)\([0-9]*\)\.\([0-9]*\)\.\([0-9]\)\(.*\)$'
LINE=`grep "$PAT" $2`
BEFORE=`echo $LINE | sed -n "s/$PAT/\1/p"`
MAJOR=`echo $LINE | sed -n "s/$PAT/\2/p"`
MINOR=`echo $LINE | sed -n "s/$PAT/\3/p"`
UPDATE=`echo $LINE | sed -n "s/$PAT/\4/p"`
AFTER=`echo $LINE | sed -n "s/$PAT/\5/p"`
if [ "X$1" == Xupdate ]; then
   NEW_VERSION="$MAJOR.$MINOR.`expr $UPDATE + 1`"
elif [ "X$1" == Xminor ]; then
   NEW_VERSION="$MAJOR.`expr $MINOR + 1`.0"
elif [ "X$1" == Xmajor ]; then
   NEW_VERSION="`expr $MAJOR + 1`.0.0"
elif [ "X$1" == Xshow ]; then
   echo "$MAJOR.$MINOR.$UPDATE"
   exit 0
else
   NEW_VERSION="$MAJOR.$MINOR.$UPDATE"
   echo "Unexpected first argument: '$1'" >&2
fi
echo "$MAJOR.$MINOR.$UPDATE -> $NEW_VERSION" >&2
sed "s|$PAT|$BEFORE$NEW_VERSION$AFTER|" $2
