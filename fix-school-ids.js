require('dotenv').config();
const { Client } = require('pg');

async function fixSchoolIds() {
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
        const res = await client.query("UPDATE users SET school_id = 1 WHERE role != 'SUPER_ADMIN' AND school_id IS NULL");
        console.log(`Successfully updated ${res.rowCount} users to have a school_id of 1.`);
    } catch (e) {
        console.error("Error:", e);
    } finally {
        await client.end();
    }
}

fixSchoolIds();
