export type Account={id:number;userId:number;accountType:string;accountNumber:string;balance:number;status:string};
export type Profile={id:number;fullName:string;email:string;role:'CUSTOMER'|'ADMIN'|'EMPLOYEE';mfaVerified:boolean;emailVerified:boolean;referralCode:string};
export type Generic={id:number;[key:string]:unknown};
let csrfToken:string|undefined;
async function csrf(){const r=await fetch('/api/auth/csrf',{credentials:'same-origin',cache:'no-store'});if(!r.ok)throw new Error('Cannot initialize security session');csrfToken=(await r.json()).token as string;}
export async function api<T=unknown>(url:string,method='GET',body?:unknown):Promise<T>{
 const hasBody=body!==undefined;
 if(method!=='GET'&&method!=='HEAD')await csrf();
 const headers:Record<string,string>={};if(csrfToken&&method!=='GET')headers['X-XSRF-TOKEN']=csrfToken;
 if(hasBody&&!(body instanceof FormData))headers['Content-Type']='application/json';
 const r=await fetch(url,{method,credentials:'same-origin',cache:'no-store',headers,body:hasBody?(body instanceof FormData?body:JSON.stringify(body)):undefined});
 if(r.status===204)return undefined as T;
 const text=await r.text();let result:unknown={};try{result=text?JSON.parse(text):{};}catch{result={error:text};}
 if(!r.ok){const obj=result as {error?:string;message?:string};throw new Error(obj.error||obj.message||`HTTP ${r.status}`);}
 return result as T;
}
export async function login(email:string,password:string){
 await csrf();const body=new URLSearchParams({email,password});
 const r=await fetch('/api/auth/login',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded','X-XSRF-TOKEN':csrfToken!},body});
 let data:{error?:string;message?:string}={};try{data=await r.json();}catch{/* empty */}
 if(!r.ok)throw new Error(data.error||'Login failed');csrfToken=undefined;return data;
}
export function money(amount:unknown){return new Intl.NumberFormat('en-US',{style:'currency',currency:'USD'}).format(Number(amount||0));}
export function requestKey(){return crypto.randomUUID();}
