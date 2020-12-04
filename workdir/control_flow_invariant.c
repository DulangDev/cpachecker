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
  for(int i = 0; i < 100; i++){
    cpu_work_no_malloc(i, 100);
  }
  for(int j = 0; j < 100; j++){
    some_other_work(1, j);
  }
}
