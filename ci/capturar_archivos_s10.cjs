// Visor de archivos reales exportados del emulador; no representa Device Explorer ni Postman.
const fs=require('fs'),path=require('path');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const root=path.resolve('evidencias/sesion10-autonoma');
const esc=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROME_PATH,headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1280,height:1100},deviceScaleFactor:1});
  for(const [file,title] of [['almacen-cifrado.xml','Almacén cifrado antes del cierre'],['almacen-despues-logout.xml','Almacén después del cierre']]) {
   const xml=fs.readFileSync(path.join(root,'android',file),'utf8');
   const strings=[...xml.matchAll(/<string name="([^"]+)">([^<]+)<\/string>/g)];
   const propias=strings.filter(x=>!x[1].startsWith('__androidx_security_crypto'));
   let html=`<h1>${title}</h1><p>Archivo real del emulador Android · pe.edu.upeu.pharmamobil</p><p>data/data/pe.edu.upeu.pharmamobil/shared_prefs/sesion_segura.xml</p><h2>Entradas de sesión: ${propias.length}</h2>`;
   html+=propias.length?propias.map(x=>`<h3>Nombre cifrado</h3><pre>${esc(x[1])}</pre><h3>Valor cifrado</h3><pre>${esc(x[2])}</pre>`).join(''):'<p>No hay entradas de sesión. Permanecen únicamente los keysets internos de AndroidX.</p>';
   html+='<details><summary>Keysets internos de AndroidX (plegados)</summary><pre>'+esc(xml)+'</pre></details><footer>Lectura real vía instrumentación/ADB; vista de archivo, no captura de Android Studio.</footer>';
   await page.setContent('<style>body{font:18px Arial;margin:38px;color:#15324a}h1{font-size:30px}h2{font-size:23px}pre{font:15px Consolas;white-space:pre-wrap;overflow-wrap:anywhere;background:#f0f4f8;padding:18px;line-height:1.5}p,footer{line-height:1.6}footer{font-size:15px;color:#526579;margin-top:30px}</style>'+html);
   await page.screenshot({path:path.join(root,file.replace('.xml','.png')),fullPage:true});
  }
  const lines=fs.readFileSync(path.join(root,'ktor-saneado.txt'),'utf8').split(/\r?\n/);
  const relevant=lines.filter(x=>x.includes('Authorization')||x.includes('REQUEST:')||x.includes('RESPONSE:'));
  const slice=relevant.slice(-55);
  await page.setContent('<style>body{font:18px Arial;margin:32px;color:#15324a}pre{font:14px Consolas;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.6;background:#f0f4f8;padding:18px}</style><h1>Registro real de Ktor</h1><p>Logcat del emulador · Últimas solicitudes de los ensayos · Authorization saneada</p><pre>'+esc(slice.join('\n'))+'</pre><p>Vista del archivo exportado mediante adb logcat. No se registra el cuerpo del login ni el JWT.</p>');
  await page.screenshot({path:path.join(root,'registro-saneado.png'),fullPage:true});
 } finally {await browser.close()}
})();
