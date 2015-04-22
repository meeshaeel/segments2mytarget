# Mailru segment exporter

## Requirements
- sbt 0.13.8

## For make executable jar:
```bash
sbt clean assembly
```

## For publish jar to Nexus repository:
Create file with Nexus credentials ~/.sbt/.credentials

```
realm=Sonatype Nexus Repository Manager
host=repo
user=username
password=password
```

And run command

```bash
sbt clean publish
```

## Launch

## Show help message:
```bash
java -jar target/scala-2.11/mailru-segment-exporter_2.11-<version>.jar --help
```

## Available options
```
   --help
         prints this usage text
   -i <workdir> | --workdir <workdir>
         Directory with files to process. ex. /opt/segments
   -p <partner> | --partner <partner>
         mailru partner prefix, will be first line of each processed file
   -o <outputname> | --outputname <outputname>
         output folder name with parsing results. 'results' by default
   -d <date> | --date <date>
         suffix of segment file name. It will be used for auditory update in future. ex. 20151231, now by default
   -r <regexp> | --regexp <regexp>
         source filename pattern in workdir, default .*(.gz)$
   -u | --upload
         upload segments to mailru or not. false by default
   -y | --auditoryupdate
         update auditories in mailru or not. false by default
   -a | --apiurl
         mailru api url, https://target.my.com by default
   -c <client> | --client <client>
         your mailru client_id
   -s <secret> | --secret <secret>
         your mailru client_secret
   -m <minion> | --minion <minion>
         subaccount name for agencies

```

### Full params for export and upload
```bash
java -jar target/scala-2.11/mailru-segment-exporter_2.11-<version>.jar --workdir /tmp/gz --outputname export-results --date 20150330 --regexp ".*(.gz)$" --partner pr --upload --client someclient --secret somesecret
```

### Full params for auditory update
```bash
java -jar target/scala-2.11/mailru-segment-exporter_2.11-<version>.jar --auditoryupdate --client someclient --secret somesecret --minion 6minion048@agency_client
```


### Simple process and upload with defaults
```bash
java -jar target/scala-2.11/mailru-segment-exporter_2.11-<version>.jar -i /tmp/gz -p dl -u -c someclient -s somesecret
```
