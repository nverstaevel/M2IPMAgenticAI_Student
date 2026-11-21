(function(){
  var K=1920,H=1080;
  // navigateurs sans Shadow DOM déclaratif : on attache les racines à la main
  document.querySelectorAll('template[shadowrootmode]').forEach(function(t){
    var h=t.parentNode; if(h.shadowRoot) return; var r=h.attachShadow({mode:'open'}); r.appendChild(t.content); t.remove();
  });
  function fit(){
    document.querySelectorAll('.slide .stage').forEach(function(st){
      var k=st.clientWidth/K, d=st.querySelector('.deck'); d.style.transform='scale('+k+')'; st.style.height=(H*k)+'px';
    });
  }
  window.addEventListener('resize',fit); window.addEventListener('load',fit); fit();
  // plein écran : une slide à la fois
  var figs=[].slice.call(document.querySelectorAll('figure.slide')), cur=-1;
  var v=document.createElement('div'); v.id='viewer'; v.innerHTML='<div class="vs"><div class="deck"></div></div><div class="vbar"><button data-a="p">←</button><span id="vn"></span><button data-a="n">→</button><button data-a="x">Fermer</button></div>';
  document.body.appendChild(v);
  function size(){ var vs=v.querySelector('.vs'),d=v.querySelector('.deck'); var k=Math.min((innerWidth-40)/K,(innerHeight-90)/H); d.style.transform='scale('+k+')'; vs.style.width=(K*k)+'px'; vs.style.height=(H*k)+'px'; }
  function show(i){
    if(i<0||i>=figs.length) return; cur=i;
    var src=figs[i].querySelector('.deck'); var vd=v.querySelector('.deck'); if(!vd.shadowRoot) vd.attachShadow({mode:'open'}); vd.shadowRoot.innerHTML=src.shadowRoot.innerHTML; vd.style.setProperty('--pg',"'"+figs[i].dataset.n+"'");
    v.querySelector('#vn').textContent=figs[i].dataset.n+' / 142'; v.classList.add('on'); size();
    history.replaceState(null,'','#'+figs[i].id);
  }
  function close(){ v.classList.remove('on'); if(cur>=0) figs[cur].scrollIntoView({block:'center'}); cur=-1; }
  figs.forEach(function(f,i){ f.querySelector('.stage').addEventListener('click',function(){show(i);}); });
  v.addEventListener('click',function(e){ var a=e.target.dataset&&e.target.dataset.a; if(a==='p')show(cur-1); else if(a==='n')show(cur+1); else if(a==='x'||e.target===v)close(); });
  document.addEventListener('keydown',function(e){ if(cur<0)return; if(e.key==='ArrowRight'||e.key==='PageDown'||e.key===' '){e.preventDefault();show(cur+1);} else if(e.key==='ArrowLeft'||e.key==='PageUp'){e.preventDefault();show(cur-1);} else if(e.key==='Escape'){close();} });
  window.addEventListener('resize',function(){ if(cur>=0)size(); });
})();
