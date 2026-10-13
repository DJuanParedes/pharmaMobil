// Evidencias reales de Swagger. Solo se redacta el JWT en el texto del curl.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('fs');const path=require('path');
(async()=>{
const out=path.resolve('evidencias/sesion10/swagger');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({executablePath:process.env.CHROME_PATH || undefined,headless:true});
try {
for(const item of [{name:'401-sin-token',id:'operations-producto-controller-listar_2',code:401},
{name:'200-admin',id:'operations-producto-controller-listar_2',code:200,user:'admin',password:process.env.PHARMA_ADMIN_PASSWORD},
{name:'403-almacen',id:'operations-cliente-controller-listar_3',code:403,user:'almacen',password:process.env.PHARMA_ALMACEN_PASSWORD}]) {
 const context=await browser.newContext({viewport:{width:1400,height:1100}});
 const page=await context.newPage();await page.goto('http://localhost:8080/swagger-ui.html');
 const op=page.locator('#'+item.id);await op.waitFor();
 if(item.user) {
  const response=await context.request.post('http://localhost:8080/api/v1/auth/login',{data:{username:item.user,password:item.password}});
  if(response.status()!==200)throw Error('Login falló');const {token}=await response.json();
  await page.locator('.scheme-container .authorize').click();
  await page.locator('.auth-container input').fill(token);
  await page.locator('.auth-btn-wrapper button').filter({hasText:/^Authorize$/}).click();
  await page.locator('.auth-btn-wrapper button').filter({hasText:/^Close$/}).click();
 }
 await op.locator('.opblock-summary').click();
 await op.getByRole('button',{name:'Try it out',exact:true}).click();
 const got=page.waitForResponse(r=>r.url().includes('/api/v1/'+(item.id.includes('cliente')?'clientes':'productos'))&&r.request().method()==='GET');
 await op.getByRole('button',{name:'Execute',exact:true}).click();
 const response=await got;if(response.status()!==item.code)throw Error(item.name+': '+response.status());
 await op.locator('.responses-inner .response-col_status').first().waitFor();
 await page.waitForTimeout(300);
 await op.locator('.curl').evaluateAll(nodes=>nodes.forEach(n=>{n.textContent=n.textContent.replace(/Bearer\s+[A-Za-z0-9_.-]+/g,'Bearer [REDACTADO]');}));
 await op.screenshot({path:path.join(out,item.name+'.png')});
 console.log(item.name+' '+response.status());await context.close();
}
}finally{await browser.close();}
})();
