import {authFetch} from './auth';
import {escapeHtml,responseError,showToast} from './ui';
export const esc=escapeHtml;
export const $=(id:string)=>document.getElementById(id)!;
export const val=(id:string)=>( $(id) as HTMLInputElement).value;
export const json=(method:string,body:unknown):RequestInit=>({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
export async function request(path:string,options:RequestInit={}){const r=await authFetch(path,options);if(!r)throw new Error('Sesión expirada');if(!r.ok)throw new Error(await responseError(r));return r;}
export async function data(path:string,options:RequestInit={}){const r=await request(path,options);const s=await r.text();return s?JSON.parse(s):null;}
export async function download(path:string,name:string,options:RequestInit={}){const r=await request(path,options);const url=URL.createObjectURL(await r.blob());const a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),30000);}
export function error(e:unknown){showToast(e instanceof Error?e.message:String(e),true);}
export function bytes(n:number){return n>=1048576?(n/1048576).toFixed(1)+' MB':n>=1024?(n/1024).toFixed(0)+' KB':n+' B';}
export function extension(n:string){return n.split('.').pop()?.toLowerCase()||'';}
export function safeUrl(s:string){try{const u=new URL(s);return u.protocol==='https:'&&!u.username&&!u.password?u.href:'';}catch{return '';}}
export function gitUrl(s:string){const u=safeUrl(s);return /^https:\/\/github\.com\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+\/?$/.test(u)?u.replace(/\/$/,''):'';}
export function dateKey(d:Date){return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;}
export async function fillCovers(root:HTMLElement){for(const img of root.querySelectorAll<HTMLImageElement>('img[data-cover]')){try{const r=await request(img.dataset.cover!);const url=URL.createObjectURL(await r.blob());img.onload=()=>URL.revokeObjectURL(url);img.onerror=()=>{URL.revokeObjectURL(url);img.remove();};img.src=url;}catch{img.remove();}}}
