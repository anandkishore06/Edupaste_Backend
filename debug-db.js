require('dotenv').config();
const { Client } = require('pg');

async function debugSessions() {
    const url = process.env.SPRING_DATASOURCE_URL;
    const cleanUrl = url.replace("jdbc:postgresql://", "");
    const [hostPort, dbWithParams] = cleanUrl.split("/");
    const db = dbWithParams.split("?")[0];
    const [host, port] = hostPort.split(":");
    
    const client = new Client({
        host: host,
        port: parseInt(port),
        database: db,
        user: process.env.SPRING_DATASOURCE_USERNAME,
        password: process.env.SPRING_DATASOURCE_PASSWORD,
        ssl: { rejectUnauthorized: false }
    });

    try {
        await client.connect();
        const users = await client.query("SELECT id, email, role, school_id FROM users WHERE email = 'schooladmin@edupaste.com'");
        console.log("Users:", users.rows);
        
        const sessions = await client.query("SELECT id, name, is_current, school_id FROM academic_sessions");
        console.log("Sessions:", sessions.rows);
    } catch (e) {
        console.error("Error:", e);
    } finally {
        await client.end();
    }
}

debugSessions();
