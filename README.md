
GODS TEAM PANEL - Java Spring Boot project (production-ready)

How to run (local):
1. Install JDK 17 and Maven.
2. mvn clean package
3. export PHONEPE_SALT="your_salt"
4. export MERCHANT_ID="your_mid"
5. java -jar target/gods-team-0.0.1-SNAPSHOT.jar
6. Open http://localhost:8080

Deploy to Render:
1. Create new Web Service on Render and connect GitHub repo (or drag & drop).
2. Set Environment variables on Render: PHONEPE_SALT, MERCHANT_ID, CALLBACK_URL
3. Deploy. Use provided README for callback setup on PhonePe.
