# <img src="./frontend/src/favicon.ico" width="40" height="40"> Tally App

The purpose of the Tally App is to digitize tally lists for the sale of, for example, beverages.
This is particularly useful in offices or clubhouses.

# System Requirements

## Supported Java Versions

To run Tally App Java 21 is required.

```bash
apt update
apt install temurin-21-jre
```

## Database Requirements

Tally App is compatible with PostgreSQL and H2.
See the provided property file for examples of the connection URLs.

PostgreSQL must be installed beforehand, and a database schema needs to be created.
In the connection URL, a user with permission to create, read and write tables in the schema must be used.
On startup, Tally App creates its tables in the specified schema.

If H2 is used, the database is created in the file specified in the connection URL.
H2 can also be declared as in-memory database.
However, using an in-memory database is strongly discouraged since all data is lost when the app stops running.

# Installation (Ubuntu)

To install Tally App, download the GZip’d TAR archive of the latest release and follow the steps below, at least for
Ubuntu.
For other systems, the installation steps for Ubuntu can be derived accordingly.

The archive contains an executable Java archive (JAR) and a YAML file with example properties and explanations.

Put both, the JAR and the property file in the directory `/opt/tallyapp/`.
Change the properties according to the requirements.

Create a user to run Tally App as a specific user.

```bash
useradd --user-group \
        --shell /bin/false \
        --expiredate 9999-12-31 \
        -M \
        tallyapp
```

To run the app a systemd service is a good choice:

```bash
vim /etc/systemd/system/tallyapp.service
```

```text
[Unit]
Description=Tally App.
Documentation=https://github.com/saschaufer/tally-app

[Service]
WorkingDirectory=/opt/tallyapp

ExecStart=/usr/lib/jvm/temurin-21-jre-amd64/bin/java \
          -jar \
          tallyapp.jar \
          --spring.config.additional-location=file:application.yml

User=tallyapp
Group=tallyapp

Type=simple

Restart=no

StandardOutput=append:/var/log/tallyapp/application.log
StandardError=inherit
StandardInput=null

[Install]
WantedBy=multi-user.target
```

Logs are written to stdout and caught by the systemd service.
This service then writes the logs to the file `/var/log/tallyapp/application.log`.

To prevent the log file from growing indefinitely the file must be rotated.
This is done by logrotate utility.

```
mkdir /var/log/tallyapp
```

```bash
vim /etc/logrotate.d/tallyapp
```

```text
/var/log/tallyapp/*.log {
        daily
        rotate 7
        compress
        create 0640 tallyapp adm
        notifempty
        missingok
        sharedscripts
        postrotate
                systemctl restart tallyapp.service
        endscript
}
```

Finally set the permissions for directories and files.

```bash
chown root: /etc/systemd/system/tallyapp.service
chmod 644 /etc/systemd/system/tallyapp.service
chown tallyapp:adm /var/log/tallyapp/
chmod 644 /var/log/tallyapp/
chown root: /etc/logrotate.d/tallyapp
chmod 644 /etc/logrotate.d/tallyapp
chown root: /opt/tallyapp/
chmod 755 /opt/tallyapp/
chown root: /opt/tallyapp/tallyapp.jar
chmod 644 /opt/tallyapp/tallyapp.jar
chown root:tallyapp /opt/tallyapp/application.yml
chmod 644 /opt/tallyapp/application.yml
```

Now Tally App can be started.

```bash
systemctl daemon-reload
systemctl enable tallyapp.service
systemctl start tallyapp.service
```

# Usage

For users to purchase products admins need to create them first.
To make a user an admin, their email address must be entered in the list of admins in the properties.
Purchases can be made by scanning a QR code.
The URL for the QR code is structured as followed:

```
https://{domain}/#/qr/{productID}
```

The product ID can be found in the product list.

If users pay their debts, they need to balance their account under payments.
This allows the correct account balance to be displayed.
