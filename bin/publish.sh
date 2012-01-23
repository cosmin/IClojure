mvn clean package

today=$(date "+%Y%m%d")
commits=$(git log --oneline | wc -l | awk '{print $1}')

filename=target/iclojure-*.jar
target=$(echo $filename | sed "s/SNAPSHOT/${today}-${commits}/")

cp $filename $target

echo ""
echo "Jar ready at: $target"
#github_upload $target
