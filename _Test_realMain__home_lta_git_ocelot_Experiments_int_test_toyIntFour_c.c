#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase5)
{
double __val0 = -65;
double __val1 = -78;
double __val2 = 47124250;
double __val3 = -243407170;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase3)
{
double __val0 = 75;
double __val1 = 81;
double __val2 = -1464000708;
double __val3 = -1358202013;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase1)
{
double __val0 = 55;
double __val1 = -31;
double __val2 = -1346324850;
double __val3 = 509603271;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase6)
{
double __val0 = -65;
double __val1 = -66;
double __val2 = 43376707;
double __val3 = -243407170;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase2)
{
double __val0 = -19;
double __val1 = -51;
double __val2 = -107138890;
double __val3 = 1469134212;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase4)
{
double __val0 = -29;
double __val1 = 49;
double __val2 = 96846345;
double __val3 = -146645163;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_e2eb1ec6(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_e2eb1ec6");

temp_tc = tcase_create("ocelot_testcase5");
tcase_add_test(temp_tc, ocelot_testcase5);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase6");
tcase_add_test(temp_tc, ocelot_testcase6);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_e2eb1ec6();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}