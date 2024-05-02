#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase1)
{
double __val0 = 26;
double __val1 = 77;
double __val2 = 505309829;
double __val3 = 1266824420;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase2)
{
double __val0 = -7;
double __val1 = -2;
double __val2 = 1048426538;
double __val3 = 1412794304;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase4)
{
double __val0 = 46;
double __val1 = -31;
double __val2 = -607829485;
double __val3 = -581341296;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase3)
{
double __val0 = 72;
double __val1 = 79;
double __val2 = -956555991;
double __val3 = -1913633260;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_fa4eb5d4(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_fa4eb5d4");

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_fa4eb5d4();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}