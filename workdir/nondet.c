int __VERIFIER_nondet_int(void);

int compute_some_values(){
  if(__VERIFIER_nondet_int()){
    return 1;
  }
  return -1;
}

void main(void){
  free((void*)0);
  int accumulated = 1;
  for(int i = 0; i < 10000; i++){
    accumulated *= compute_some_values();
    if(accumulated > 0){
      free(0);
    }
  }
}