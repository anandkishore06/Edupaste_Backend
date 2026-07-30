require('dotenv').config();
const { Client } = require('pg');
const bcrypt = require('bcryptjs');

async function updatePassword() {
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
        
        // Hash 'password123' using bcrypt with cost factor 10 (which Spring Security uses by default)
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash("password123", salt);
        
        console.log("Hashed password:", hashedPassword);
        
        const insertQuery = `
            INSERT INTO users (full_name, email, password, role) 
            VALUES ($1, $2, $3, $4)
            ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password
        `;
        
        await client.query(insertQuery, ['Super Admin', 'superadmin@edupaste.com', hashedPassword, 'SUPER_ADMIN']);
        console.log("✅ Super Admin created (or updated) successfully with email: superadmin@edupaste.com and password: password123");
        
    } catch (err) {
        console.error(err);
    } finally {
        await client.end();
    }
}

updatePassword();
