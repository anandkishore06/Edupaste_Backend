const { Client } = require('pg');
const bcrypt = require('bcryptjs');

async function fixPassword() {
  const client = new Client({
    connectionString: 'postgresql://postgres.kkweqesfxxnmdhsziycj:Edupaste7764@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?prepareThreshold=0',
    ssl: { rejectUnauthorized: false }
  });

  await client.connect();
  
  // Generate BCrypt hash with $2a$ prefix (Spring Boot compatible)
  const hash = bcrypt.hashSync('password123', 10).replace('$2b$', '$2a$');
  console.log('Generated hash:', hash);
  
  const result = await client.query(
    "UPDATE users SET password = $1 WHERE email = 'superadmin@edupaste.com'",
    [hash]
  );
  console.log('Updated rows:', result.rowCount);
  
  // Verify the stored hash
  const verify = await client.query(
    "SELECT password FROM users WHERE email = 'superadmin@edupaste.com'"
  );
  console.log('Stored hash:', verify.rows[0].password);
  
  await client.end();
}

fixPassword().catch(console.error);
