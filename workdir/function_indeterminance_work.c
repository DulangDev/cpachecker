int __VERIFIER_nondet_int(void);
int ** big_array [3000];
int  cpu_work_no_malloc(int start, int size){
  for(int i = start+1; i < start+size; i++){
       big_array[i] = big_array[i-1];
    }
   return -1;
}

int some_other_work(int i, int j){
  for(; i < j; i++){
    big_array[i] = big_array[j];
  }
}

int main(void){
  for(int _ = 0; _ < 999; _++){
    if(__VERIFIER_nondet_int()){
      cpu_work_no_malloc(_, 999);
    } else {
      some_other_work(999, _);
    }
  }
}
