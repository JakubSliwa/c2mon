package cern.c2mon.web.manager.config;

import cern.c2mon.web.manager.security.RbacAuthenticationProvider;
import cern.c2mon.web.manager.security.RbacDecisionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Justin Lewis Salmon
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private Environment environment;

  @Autowired
  private RbacAuthenticationProvider rbacAuthenticationProvider;

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/static/**").antMatchers("/css/**").antMatchers("/js/**").antMatchers("/img/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        .authenticationProvider(rbacAuthenticationProvider)
        .authorizeRequests()
          .accessDecisionManager(rbacDecisionManager())
          .antMatchers("/login").permitAll()
          .antMatchers("/configloader/progress").hasRole("ADMIN")
          .antMatchers("/process/**").hasRole("ADMIN")
          .antMatchers("/commandviewer/**").hasRole("ADMIN")
          .anyRequest().anonymous()
        .and()
        .formLogin()
          .loginPage("/login")
          .loginProcessingUrl("/login")
          .failureUrl("/login?error=true")
          .permitAll()
        .and()
        .csrf().disable();
  }

  @Bean
  public AccessDecisionManager rbacDecisionManager() {
    Map<String, String> authorisationDetails = new HashMap<>();
    authorisationDetails.put("configloader/progress", environment.getProperty("c2mon.web.rbac.admin"));
    authorisationDetails.put("process", environment.getProperty("c2mon.web.rbac.user"));
    authorisationDetails.put("commandviewer", environment.getProperty("c2mon.web.rbac.user"));

    return new RbacDecisionManager(authorisationDetails);
  }
}
