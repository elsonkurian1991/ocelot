
int bestFitness_functionA_1  = 100000;
int bestFitness_functionA_2  = 100000;
int bestFitness_functionA_3  = 100000;
int currentFitnessfunctionA_2 = (0 < x ? 0 : abs(0 - x) + 0.01);  if (numOfCall£funName£ == 0) {
FILE *fptr1 = fopen("evalfunctionAPC1.epc","w");
 fprintf(fptr1, "%d
", currentFitnessfunctionA_1);
 fclose(fptr1);
FILE *fptr2 = fopen("evalfunctionAPC2.epc","w");
 fprintf(fptr2, "%d
", currentFitnessfunctionA_2);
 fclose(fptr2);
FILE *fptr3 = fopen("evalfunctionAPC3.epc","w");
 fprintf(fptr3, "%d
", currentFitnessfunctionA_3);
 fclose(fptr3);
}
 else {if (currentFitnessfunctionA_1 < bestFitnessfunctionA_1) {
FILE *fptr1 = fopen("evalfunctionAPC1.epc","w");
 fprintf(fptr1, "%d
", currentFitnessfunctionA_1);
 fclose(fptr1);if (currentFitnessfunctionA_1 < bestFitnessfunctionA_1) {
}if (currentFitnessfunctionA_2 < bestFitnessfunctionA_2) {
FILE *fptr2 = fopen("evalfunctionAPC2.epc","w");
 fprintf(fptr2, "%d
", currentFitnessfunctionA_2);
 fclose(fptr2);if (currentFitnessfunctionA_2 < bestFitnessfunctionA_2) {
}if (currentFitnessfunctionA_3 < bestFitnessfunctionA_3) {
FILE *fptr3 = fopen("evalfunctionAPC3.epc","w");
 fprintf(fptr3, "%d
", currentFitnessfunctionA_3);
 fclose(fptr3);if (currentFitnessfunctionA_3 < bestFitnessfunctionA_3) {
}