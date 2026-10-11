export function escapeHtml(v:unknown):string {return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]??c));}
export function fecha(v:string):string {if(!v)return '';const d=new Date(v);return Number.isNaN(d.getTime())?'':d.toLocaleDateString('es-PE',{day:'numeric',month:'short',year:'numeric'});}
export function initials(name:string):string {return (name||'UTP').split(' ').filter(Boolean).slice(0,2).map(v=>v[0]).join('').toUpperCase();}
export function showToast(msg:string,error=false){const el=document.getElementById('toast');if(!el){alert(msg);return;}el.textContent=msg;el.className='toast show'+(error?' error':'');setTimeout(()=>el.className='toast',5000);}
export async function responseError(r:Response):Promise<string> {try {const data=await r.json();return data.message??data.detail??data.error??'Error inesperado';}catch{return 'Error en la solicitud ('+r.status+')';}}
export const categories:{[k:string]:string}={PREGUNTAS:'Preguntas y respuestas',PROFESORES:'Profesores',CURSOS:'Cursos'};
