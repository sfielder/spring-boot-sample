spring-boot-sample
==================

1.  Set the environment variable `DATABASE_URL`="postgres://username:password@hostname/db"

2.  Enable the SMS add-on by running `heroku addons:add blowerio`

3.  Run `mvn package` to create `target/spring-boot-example-1.0-SNAPSHOT.war`

4.  Start the app with `foreman start`
