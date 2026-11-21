(function(){
const root=document.body.dataset.root||"";
const K='m2ipm:checks';
function load(){try{return JSON.parse(localStorage.getItem(K)||'{}')}catch(e){return {}}}
function save(o){try{localStorage.setItem(K,JSON.stringify(o))}catch(e){}}
// thème
document.getElementById('btn-theme').onclick=()=>{const d=document.documentElement,cur=d.dataset.theme||(matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light');const n=cur==='dark'?'light':'dark';d.dataset.theme=n;try{localStorage.setItem('m2ipm:theme',n)}catch(e){}};
// progression
function pct(prefix,total){const o=load();let n=0;for(const k in o)if(o[k]&&k.startsWith(prefix))n++;return total?Math.min(100,Math.round(100*n/total)):0}
function paint(){document.querySelectorAll('[data-prefix][data-total]').forEach(el=>{const p=pct(el.dataset.prefix,+el.dataset.total);const i=el.querySelector('.bar i'),t=el.querySelector('.pct');if(i)i.style.width=p+'%';if(t)t.textContent=p+' %'})}
const st=load();
document.querySelectorAll('input[data-k]').forEach(c=>{c.checked=!!st[c.dataset.k];c.addEventListener('change',()=>{const o=load();if(c.checked)o[c.dataset.k]=1;else delete o[c.dataset.k];save(o);paint()})});
paint();
// copier le code
document.querySelectorAll('.codehilite,pre').forEach(b=>{if(b.closest('.codehilite')&&b.tagName==='PRE')return;const btn=document.createElement('button');btn.className='copy';btn.textContent='Copier';btn.onclick=()=>{const t=(b.querySelector('code')||b).innerText;navigator.clipboard&&navigator.clipboard.writeText(t).then(()=>{btn.textContent='Copié ✓';setTimeout(()=>btn.textContent='Copier',1400)})};b.appendChild(btn)});
// sommaire : section courante
const links=[...document.querySelectorAll('.toc a')];
if(links.length){const map=new Map(links.map(a=>[a.getAttribute('href').slice(1),a]));const io=new IntersectionObserver(es=>{es.forEach(e=>{if(e.isIntersecting){links.forEach(l=>l.classList.remove('on'));const a=map.get(e.target.id);a&&a.classList.add('on')}})},{rootMargin:'-80px 0px -70% 0px'});map.forEach((a,id)=>{const h=document.getElementById(id);h&&io.observe(h)})}
// menu TD : fermer au clic ailleurs
document.addEventListener('click',e=>{document.querySelectorAll('.menu[open]').forEach(m=>{if(!m.contains(e.target))m.removeAttribute('open')})});
// recherche
const box=document.getElementById('search'),q=document.getElementById('q'),res=document.getElementById('res');let idx=null,sel=0;
const norm=s=>s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g,'');
async function ensure(){if(idx)return;try{idx=(await (await fetch(root+'assets/search-index.json')).json()).map(d=>({...d,n:norm(d.t),b:norm(d.x)}))}catch(e){idx=[]}}
function openS(){box.hidden=false;q.value='';res.innerHTML='';q.focus();ensure()}
function closeS(){box.hidden=true}
document.getElementById('btn-search').onclick=openS;
document.addEventListener('keydown',e=>{if(e.key==='/'&&!/INPUT|TEXTAREA/.test(document.activeElement.tagName)){e.preventDefault();openS()}if(e.key==='Escape')closeS();
 if(!box.hidden&&(e.key==='ArrowDown'||e.key==='ArrowUp')){const a=[...res.querySelectorAll('a')];if(!a.length)return;e.preventDefault();sel=(sel+(e.key==='ArrowDown'?1:-1)+a.length)%a.length;a.forEach((x,i)=>x.classList.toggle('sel',i===sel))}
 if(!box.hidden&&e.key==='Enter'){const a=res.querySelectorAll('a')[sel];a&&(location.href=a.href)}});
box.addEventListener('click',e=>{if(e.target===box)closeS()});
q.addEventListener('input',async()=>{await ensure();const terms=norm(q.value).split(/\s+/).filter(Boolean);if(!terms.length){res.innerHTML='';return}
 const sc=idx.map(d=>{let s=0;for(const t of terms){if(d.n.includes(t))s+=10;else if(d.b.includes(t))s+=1;else return null}return[d,s]}).filter(Boolean).sort((a,b)=>b[1]-a[1]).slice(0,9);
 sel=0;res.innerHTML=sc.length?sc.map(([d],i)=>`<li><a class="${i?'':'sel'}" href="${root+d.u}"><b>${d.g}</b>${d.t}</a></li>`).join(''):'<li><small>Aucun résultat</small></li>'});
})();
