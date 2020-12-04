// need to look at cache_misses, = 3
int __VERIFIER_nondet_int(void);
int ** big_array [3000];
int  cpu_work_no_malloc(int start, int size){
  for(int i = start+1; i < start+size; i++){
       big_array[i] = big_array[i-1];
    }
   return __VERIFIER_nondet_int();
}

int main(void){
  for(int i = 0; i < 1000; i++){
    cpu_work_no_malloc(i, 10);
  }

  for(int i = 1001; i < 1999; i++){
    cpu_work_no_malloc(i, 100);
  }
}