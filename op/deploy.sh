# Local env -> build and deploy

/Users/avilches/Work/Proy/Local/filesrv/gradlew assemble
scp /Users/avilches/Work/Proy/Local/filesrv/build/libs/filesrv.jar avilches@hyperspin-online.com:/srv/mamespin/bin

scp /Users/avilches/Work/Proy/Local/filesrv/op/service/filesrv.* avilches@hyperspin-online.com:/srv/mamespin/bin