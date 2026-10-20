const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('fs'); const path=require('path');
(async()=>{
 const out=path.resolve('evidencias/sesion11/swagger');fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch({executablePath:process.env.CHROME_PATH,headless:true});
 try {
  const ctx=await browser.newContext({viewport:{width:1300,height:1050}});
  const login=await ctx.request.post('http://localhost:8080/api/v1/auth/login',{data:{username:'admin',password:process.env.PHARMA_ADMIN_PASSWORD}});
  if(login.status()!==200)throw Error('Login no disponible');
  const {token}=await login.json();const page=await ctx.newPage();await page.goto('http://localhost:8080/swagger-ui.html');
  await page.locator('.scheme-container .authorize').click();await page.locator('.auth-container input').fill(token);
  await page.locator('.auth-btn-wrapper button').filter({hasText:/^Authorize$/}).click();await page.locator('.auth-btn-wrapper button').filter({hasText:/^Close$/}).click();
  const op=page.locator('#operations-producto-controller-buscar');await op.locator('.opblock-summary').click();
  await op.getByRole('button',{name:'Try it out',exact:true}).click();
  await op.locator('input').first().fill(process.env.PHARMA_PRODUCTO_ID);
  const responsePromise=page.waitForResponse(r=>r.url().endsWith('/api/v1/productos/'+process.env.PHARMA_PRODUCTO_ID)&&r.request().method()==='GET');
  await op.getByRole('button',{name:'Execute',exact:true}).click();const response=await responsePromise;
  if(response.status()!==200)throw Error('GET no respondió 200');
  await op.locator('.responses-inner .response-col_status').first().waitFor();
  await page.waitForTimeout(500);
  await op.locator('.curl').evaluateAll(nodes=>nodes.forEach(n=>{n.textContent=n.textContent.replace(/Bearer\s+[A-Za-z0-9_.-]+/g,'Bearer [REDACTADO]');}));
  await op.screenshot({path:path.join(out,'05-producto-id-positivo.png')});
  fs.writeFileSync(path.join(out,'respuesta-get.json'),JSON.stringify(await response.json(),null,2));
  console.log('Swagger GET /productos/'+process.env.PHARMA_PRODUCTO_ID+': 200; captura guardada.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e.message);process.exit(1)});
