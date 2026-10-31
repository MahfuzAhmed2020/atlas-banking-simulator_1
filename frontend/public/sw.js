// Never cache banking API responses, authenticated pages, or financial data.
const CACHE='atlas-static-v1';
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(['/icon.svg','/manifest.webmanifest'])));self.skipWaiting();});
self.addEventListener('activate',e=>{e.waitUntil(self.clients.claim());});
self.addEventListener('fetch',e=>{const u=new URL(e.request.url);if(u.origin!==location.origin||u.pathname.startsWith('/api/')||u.pathname.startsWith('/swagger')||u.pathname.startsWith('/v3/')||e.request.mode==='navigate')return;
  if(u.pathname==='/icon.svg'||u.pathname==='/manifest.webmanifest')e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request)));
});
