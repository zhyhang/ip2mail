# Scheduled lookup local IPs and send to email
## Feature
- Every 5 minute to run the task
- Any following condition occur will send email:
  - firstly run
  - ips changed
  - exceeding 6 hours from last sent

## Build
```shell
mvn clean package
```

## Run
```shell
java -jar ip2email.jar
```

## Windows Service
- download apache daemon
  [prunsrv](https://downloads.apache.org/commons/daemon/binaries/windows/)
- create system service in windows  [run success only once]  
**Run in cmd or powershell with Administer privilege**
```shell
path_to\prunsrv.exe install your_service_name --DisplayName="your_service_dispaly_name" --Description="your_service_dispaly_description" --Startup=auto --StartMode=Java --StartClass=org.yanhuang.tools.ip2mail.LookupSend --Classpath	="absolution_path_to\ip2email.jar" --StartPath="working_directory" --LogPath="absolution_path_to_prunsrv_service"
```
if create service success, will autostart with Windows system start, and collect local ips and send to email box in cycle (every 5 minute).