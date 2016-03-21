COPYRIGHT=`sed -n '1,4s/<!--\(.*\)-->/\1/p' $1`
echo "#
#$COPYRIGHT
#
# Twidlit default preferences
#" > $2
# include a bogus tag to test badtags.sh
echo test.pref.not.in.code value >> $2
sed -n 's|^<dt><tt>\([^<]*\)</tt><br>|\1|p' $1 | sed 's/\&gt/>/
s/\&lt/</' >> $2
unix2dos.exe $2 2> /dev/null
