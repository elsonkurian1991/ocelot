#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase3)
{
double __val0 = 60;
double __val1 = 94;
double __val2 = 936640814;
double __val3 = 465775906;



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
double __val0 = 98;
double __val1 = -69;
double __val2 = 1849861992;
double __val3 = 1608102080;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase1)
{
double __val0 = -83;
double __val1 = -48;
double __val2 = -370999170;
double __val3 = 1092756813;



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
double __val1 = -49;
double __val2 = -504517830;
double __val3 = 672616903;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_b2664c43(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_b2664c43");

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_b2664c43();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}