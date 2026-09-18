const DATA_ENDPOINT="https://ccsxtbpxmwnyrrmtunyf.supabase.co/functions/v1/business-data";
const BOOKING_ENDPOINT="https://ccsxtbpxmwnyrrmtunyf.supabase.co/functions/v1/public-booking";
const params=new URLSearchParams(location.search);
const pathSlug=location.pathname.split("/").filter(Boolean).pop();
const slug=params.get("slug")||location.hash.replace(/^#/,"")||(pathSlug&&pathSlug!=="Chst"&&pathSlug!=="index.html"?pathSlug:"");
const state={business:null,services:[],portfolio:[],professionals:[],step:0,service:null,professional:null,date:null,slot:null,slots:[],submitting:false};
const $=id=>document.getElementById(id);
const esc=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[c]));
const money=cents=>new Intl.NumberFormat("pt-BR",{style:"currency",currency:"BRL"}).format((Number(cents)||0)/100);
const phoneMask=value=>{const d=String(value).replace(/\D/g,"").slice(0,11);if(d.length<=2)return d;if(d.length<=7)return "("+d.slice(0,2)+") "+d.slice(2);return "("+d.slice(0,2)+") "+d.slice(2,7)+"-"+d.slice(7)};
const days=()=>Array.from({length:Math.min(Number(state.business?.booking_window_days||30),14)},(_,i)=>{const d=new Date();d.setDate(d.getDate()+i);return{key:d.toLocaleDateString("en-CA"),weekday:new Intl.DateTimeFormat("pt-BR",{weekday:"short"}).format(d).replace(".",""),day:String(d.getDate()).padStart(2,"0"),month:new Intl.DateTimeFormat("pt-BR",{month:"short"}).format(d).replace(".","")}});

async function fetchJson(url,options){
  const response=await fetch(url,options);
  const data=await response.json().catch(()=>({}));
  if(!response.ok)throw new Error(data.error||"Não foi possível continuar");
  return data;
}

async function boot(){
  if(!slug)throw new Error("Esse link não informou qual estabelecimento abrir.");
  const payload=await fetchJson(DATA_ENDPOINT+"?slug="+encodeURIComponent(slug));
  Object.assign(state,{business:payload.business,services:payload.services||[],portfolio:payload.portfolio||[],professionals:payload.professionals||[]});
  document.title=state.business.name+" · Agendivo";
  renderPage();
  renderBookingData();
}

function renderPage(){
  const b=state.business;
  const loc=[b.city,b.state].filter(Boolean).join(" · ");
  const phone=String(b.phone||"").replace(/\D/g,"");
  const wa=phone?"https://wa.me/"+(phone.startsWith("55")?phone:"55"+phone):"";

  const services=state.services.map(s=>'<button class="service" data-service="'+s.id+'">'+
    (s.image_url?'<img src="'+esc(s.image_url)+'" alt="">':'<div class="service-fallback">A</div>')+
    '<div class="service-body"><strong>'+esc(s.name)+'</strong><p>'+esc(s.description||"Serviço disponível para agendamento.")+'</p><small>'+esc(s.duration_minutes)+' min</small></div>'+
    '<div class="service-price">'+money(s.price_cents)+'</div></button>').join("");

  const portfolio=state.portfolio.map(p=>'<figure><img src="'+esc(p.image_url)+'" alt="'+esc(p.title||"Portfólio")+'">'+
    ((p.title||p.caption)?'<figcaption><strong>'+esc(p.title||"")+'</strong><span>'+esc(p.caption||"")+'</span></figcaption>':"")+
    '</figure>').join("");

  $("app").innerHTML='<main class="shell"><div class="cover"'+
    (b.cover_url?' style="background-image:linear-gradient(180deg,rgba(0,0,0,.03),#050505 100%),url(\''+esc(b.cover_url)+'\')"':'')+
    '><div class="topbrand"><b>A✦</b> AGENDIVO</div></div><div class="content"><section class="profile">'+
    (b.logo_url?'<img class="logo" src="'+esc(b.logo_url)+'" alt="">':'<div class="logo">'+esc(b.name.slice(0,1).toUpperCase())+'</div>')+
    '<div><p class="eyebrow">'+esc(b.category)+'</p><h1>'+esc(b.name)+'</h1><p class="meta">'+esc(loc)+'</p></div></section>'+
    (b.description?'<p class="description">'+esc(b.description)+'</p>':'')+
    '<div class="trust"><span class="pill">✓ Agendamento online</span><span class="pill">Sem precisar baixar app</span>'+
    (wa?'<a class="pill" href="'+wa+'">WhatsApp</a>':'')+
    '</div><div class="section-head"><h2>Serviços</h2><span>'+state.services.length+' opções</span></div><div class="services">'+
    (services||'<div class="empty">Nenhum serviço publicado ainda.</div>')+'</div>'+
    (portfolio?'<div class="section-head"><h2>Portfólio</h2><span>Trabalhos recentes</span></div><div class="portfolio">'+portfolio+'</div>':'')+
    '</div></main><div class="sticky"><div class="sticky-copy"><small>Agende sem criar conta</small><strong>Escolha seu melhor horário</strong></div><button class="primary" id="openBooking">Agendar agora</button></div>';

  $("openBooking").onclick=openBooking;
  document.querySelectorAll("[data-service]").forEach(btn=>btn.onclick=()=>{
    state.service=state.services.find(s=>s.id===btn.dataset.service);
    openBooking();
    renderBookingData();
  });
}

function renderBookingData(){
  $("serviceChoices").innerHTML=state.services.map(s=>'<button class="choice '+(state.service?.id===s.id?"active":"")+'" data-svc="'+s.id+'"><span><strong>'+esc(s.name)+'</strong><small>'+esc(s.duration_minutes)+' min</small></span><b>'+money(s.price_cents)+'</b></button>').join("")||'<div class="empty">Não há serviços disponíveis.</div>';
  document.querySelectorAll("[data-svc]").forEach(btn=>btn.onclick=()=>{state.service=state.services.find(s=>s.id===btn.dataset.svc);state.slot=null;renderBookingData()});

  $("professionalChoices").innerHTML=state.professionals.map(p=>'<button class="choice '+(state.professional?.id===p.id?"active":"")+'" data-pro="'+p.id+'"><span><strong>'+esc(p.name)+'</strong><small>'+esc(p.role_title||"Profissional")+'</small></span><b>›</b></button>').join("")||'<div class="empty">Nenhum profissional disponível.</div>';
  document.querySelectorAll("[data-pro]").forEach(btn=>btn.onclick=()=>{state.professional=state.professionals.find(p=>p.id===btn.dataset.pro);state.slot=null;renderBookingData();if(state.date)loadSlots()});

  $("days").innerHTML=days().map(d=>'<button class="day '+(state.date===d.key?"active":"")+'" data-day="'+d.key+'"><small>'+esc(d.weekday)+'</small><strong>'+d.day+'</strong><span>'+esc(d.month)+'</span></button>').join("");
  document.querySelectorAll("[data-day]").forEach(btn=>btn.onclick=()=>{state.date=btn.dataset.day;state.slot=null;renderBookingData();loadSlots()});

  renderSlots();
  renderSummary();
}

function renderSlots(){
  if(!state.service||!state.professional||!state.date){
    $("slots").innerHTML='<div class="empty" style="grid-column:1/-1">Escolha serviço, profissional e dia para ver os horários.</div>';
    return;
  }
  if(state.slots==="loading"){
    $("slots").innerHTML='<div class="empty" style="grid-column:1/-1">Consultando horários disponíveis...</div>';
    return;
  }
  const list=Array.isArray(state.slots)?state.slots:[];
  $("slots").innerHTML=list.length?list.map(iso=>'<button class="slot '+(state.slot===iso?"active":"")+'" data-slot="'+iso+'">'+new Date(iso).toLocaleTimeString("pt-BR",{hour:"2-digit",minute:"2-digit"})+'</button>').join(""):'<div class="empty" style="grid-column:1/-1">Sem horários livres neste dia. Escolha outra data.</div>';
  document.querySelectorAll("[data-slot]").forEach(btn=>btn.onclick=()=>{state.slot=btn.dataset.slot;renderSlots();renderSummary()});
}

async function loadSlots(){
  if(!state.service||!state.professional||!state.date)return;
  state.slots="loading";
  renderSlots();
  try{
    const data=await fetchJson(BOOKING_ENDPOINT+"?professional_id="+encodeURIComponent(state.professional.id)+"&service_id="+encodeURIComponent(state.service.id)+"&date="+encodeURIComponent(state.date));
    state.slots=data.slots||[];
  }catch(error){
    state.slots=[];
    alert(error.message);
  }
  renderSlots();
}

function renderSummary(){
  if(!$("summary"))return;
  const time=state.slot?new Date(state.slot).toLocaleString("pt-BR",{day:"2-digit",month:"long",hour:"2-digit",minute:"2-digit"}):"—";
  $("summary").innerHTML='<div><span>Serviço</span><strong>'+esc(state.service?.name||"—")+'</strong></div>'+
    '<div><span>Profissional</span><strong>'+esc(state.professional?.name||"—")+'</strong></div>'+
    '<div><span>Horário</span><strong>'+esc(time)+'</strong></div>'+
    '<div><span>Valor</span><strong style="color:var(--gold2)">'+(state.service?money(state.service.price_cents):"—")+'</strong></div>';
}

function openBooking(){
  if(!state.business.accepts_online_booking){
    alert("Este estabelecimento não está aceitando agendamentos online agora.");
    return;
  }
  if(!state.service&&state.services[0])state.service=state.services[0];
  state.step=0;
  showStep();
  $("bookingSheet").classList.add("open");
  $("bookingSheet").setAttribute("aria-hidden","false");
  document.body.style.overflow="hidden";
}

function closeBooking(){
  $("bookingSheet").classList.remove("open");
  $("bookingSheet").setAttribute("aria-hidden","true");
  document.body.style.overflow="";
}

function showStep(){
  document.querySelectorAll(".step").forEach((el,i)=>el.classList.toggle("active",i===state.step));
  document.querySelectorAll(".step-dot").forEach((el,i)=>el.classList.toggle("active",i<=Math.min(state.step,3)));
  $("sheetActions").style.display=state.step===4?"none":"grid";
  $("backStep").style.visibility=state.step===0?"hidden":"visible";
  $("nextStep").textContent=state.step===3?(state.submitting?"Confirmando...":"Confirmar agendamento"):"Continuar";
  renderBookingData();
}

function validateStep(){
  if(state.step===0&&!state.service){alert("Escolha um serviço.");return false}
  if(state.step===1&&!state.professional){alert("Escolha um profissional.");return false}
  if(state.step===2&&!state.slot){alert("Escolha um horário.");return false}
  if(state.step===3){
    const name=$("guestName").value.trim();
    const phone=$("guestPhone").value.replace(/\D/g,"");
    if(name.length<2){alert("Digite seu nome.");return false}
    if(phone.length<8){alert("Digite um WhatsApp válido.");return false}
  }
  return true;
}

async function next(){
  if(!validateStep())return;
  if(state.step<3){
    state.step++;
    if(state.step===2&&!state.date){
      state.date=days()[0]?.key;
      if(state.professional&&state.service)loadSlots();
    }
    showStep();
    return;
  }
  if(state.submitting)return;
  state.submitting=true;
  showStep();

  try{
    const result=await fetchJson(BOOKING_ENDPOINT,{
      method:"POST",
      headers:{"Content-Type":"application/json"},
      body:JSON.stringify({
        business_id:state.business.id,
        professional_id:state.professional.id,
        service_id:state.service.id,
        starts_at:state.slot,
        guest_name:$("guestName").value.trim(),
        guest_phone:$("guestPhone").value.trim(),
        notes:$("guestNotes").value.trim()
      })
    });

    state.step=4;
    $("successMessage").textContent=result.message||"O estabelecimento recebeu seu agendamento.";
    const dt=new Date(result.appointment.starts_at).toLocaleString("pt-BR",{weekday:"long",day:"2-digit",month:"long",hour:"2-digit",minute:"2-digit"});
    $("successSummary").innerHTML='<div><span>Serviço</span><strong>'+esc(result.service.name)+'</strong></div>'+
      '<div><span>Profissional</span><strong>'+esc(result.professional.name)+'</strong></div>'+
      '<div><span>Quando</span><strong>'+esc(dt)+'</strong></div>'+
      '<div><span>Valor</span><strong style="color:var(--gold2)">'+money(result.service.price_cents)+'</strong></div>';
  }catch(error){
    alert(error.message);
    if(error.message.toLowerCase().includes("horário")){
      state.step=2;
      await loadSlots();
    }
  }finally{
    state.submitting=false;
    showStep();
  }
}

$("guestPhone").addEventListener("input",event=>{event.target.value=phoneMask(event.target.value)});
$("closeSheet").onclick=closeBooking;
$("finishBooking").onclick=closeBooking;
$("backStep").onclick=()=>{if(state.step>0){state.step--;showStep()}};
$("nextStep").onclick=next;
$("bookingSheet").addEventListener("click",event=>{if(event.target===$("bookingSheet"))closeBooking()});

boot().catch(error=>{
  $("app").innerHTML='<section class="fatal"><div><div class="mark">A✦</div><h2>Não foi possível abrir este perfil.</h2><p>'+esc(error.message)+'</p></div></section>';
});