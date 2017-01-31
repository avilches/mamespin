# Install the fileserv as service

sudo chmod a+x /srv/mamespin/bin/filesrv.sh
sudo mv /srv/mamespin/bin/filesrv.service /etc/systemd/system/filesrv.service
sudo systemctl daemon-reload
sudo systemctl enable filesrv
