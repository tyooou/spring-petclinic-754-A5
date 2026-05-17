/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles HTTP requests related to {@link Owner} management, including creation, search,
 * update, and detail display. Supports paginated search by last name.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private static final String OWNER_NOT_FOUND_MESSAGE = "Owner not found with id: ";

	private final OwnerRepository owners;

	private static OwnerDto toDto(Owner owner) {
		OwnerDto dto = new OwnerDto();
		dto.setId(owner.getId());
		dto.setFirstName(owner.getFirstName());
		dto.setLastName(owner.getLastName());
		dto.setAddress(owner.getAddress());
		dto.setCity(owner.getCity());
		dto.setTelephone(owner.getTelephone());
		dto.setPets(owner.getPets().stream().map(OwnerController::toDto).toList());
		return dto;
	}

	private static OwnerDto.PetDto toDto(Pet pet) {
		OwnerDto.PetDto dto = new OwnerDto.PetDto();
		dto.setId(pet.getId());
		dto.setName(pet.getName());
		dto.setBirthDate(pet.getBirthDate());
		dto.setType(pet.getType() != null ? pet.getType().getName() : null);
		dto.setVisits(pet.getVisits().stream().map(OwnerController::toDto).toList());
		return dto;
	}

	private static OwnerDto.VisitDto toDto(Visit visit) {
		OwnerDto.VisitDto dto = new OwnerDto.VisitDto();
		dto.setId(visit.getId());
		dto.setDate(visit.getDate());
		dto.setDescription(visit.getDescription());
		return dto;
	}

	private static Owner toOwner(OwnerDto dto) {
		Owner owner = new Owner();
		owner.setId(dto.getId());
		owner.setFirstName(dto.getFirstName());
		owner.setLastName(dto.getLastName());
		owner.setAddress(dto.getAddress());
		owner.setCity(dto.getCity());
		owner.setTelephone(dto.getTelephone());
		return owner;
	}

	private static void updateOwnerFromDto(OwnerDto dto, Owner owner) {
		owner.setFirstName(dto.getFirstName());
		owner.setLastName(dto.getLastName());
		owner.setAddress(dto.getAddress());
		owner.setCity(dto.getCity());
		owner.setTelephone(dto.getTelephone());
	}

	public OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	/**
	 * Prevents binding of {@code id} fields to guard against mass-assignment attacks.
	 * @param dataBinder the data binder to configure
	 */
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Resolves the {@link Owner} model attribute for requests that include an
	 * {@code ownerId} path variable. Returns a new, empty {@link Owner} when no ID is
	 * present (e.g. during owner creation).
	 * @param ownerId the owner's primary key, or {@code null} on creation forms
	 * @return the existing owner, or a new {@link Owner} instance
	 */
	@ModelAttribute("owner")
	public OwnerDto findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new OwnerDto()
				: toDto(this.owners.findById(ownerId)
					.orElseThrow(() -> new IllegalArgumentException(OWNER_NOT_FOUND_MESSAGE + ownerId
							+ ". Please ensure the ID is correct " + "and the owner exists in the database.")));
	}

	/**
	 * Displays the owner creation form.
	 * @return the logical view name for the create/update form
	 */
	@GetMapping("/owners/new")
	public String initCreationForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists a new owner. Redirects to the owner's detail page on
	 * success, or returns the form with an error message on validation failure.
	 * @param owner the owner to create, populated from the submitted form
	 * @param result binding and validation results
	 * @param redirectAttributes flash attributes for the redirect
	 * @return a redirect to the new owner's detail page, or the form view on error
	 */
	@PostMapping("/owners/new")
	public String processCreationForm(@Valid @ModelAttribute("owner") OwnerDto dto, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		Owner owner = toOwner(dto);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		return "redirect:/owners/" + owner.getId();
	}

	/**
	 * Displays the owner search form.
	 * @return the logical view name for the find-owners page
	 */
	@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	/**
	 * Searches owners by last name with pagination. Redirects directly to the owner
	 * detail page when exactly one match is found, or lists all matches when multiple
	 * owners are returned. An empty last name returns all owners.
	 * @param page the 1-based page number to display (defaults to 1)
	 * @param owner used to extract the last-name search criterion
	 * @param result binding result used to report "not found" errors
	 * @param model the model to populate with paginated results
	 * @return a redirect to the single match, the list view, or the search form on error
	 */
	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, @ModelAttribute("owner") OwnerDto dto,
			BindingResult result, Model model) {
		// allow parameterless GET request for /owners to return all records
		String lastName = dto.getLastName();
		if (lastName == null) {
			lastName = ""; // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, lastName);
		if (ownersResults.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			Owner owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}

		// multiple owners found
		return addPaginationModel(page, model, ownersResults);
	}

	/**
	 * Adds paginated owner data to the model and returns the owner list view.
	 * @param page the current 1-based page number
	 * @param model the model to populate
	 * @param paginated the paginated owners result
	 * @return the logical view name for the owners list page
	 */
	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		List<OwnerDto> listOwners = paginated.getContent().stream().map(OwnerController::toDto).toList();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}

	/**
	 * Queries for a page of owners whose last name starts with the given prefix.
	 * @param page the 1-based page number
	 * @param lastname the last-name prefix to filter by (empty string returns all)
	 * @return a page of matching {@link Owner} records
	 */
	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastNameStartingWith(lastname, pageable);
	}

	/**
	 * Displays the owner update form, pre-populated with the current owner data resolved
	 * by {@link #findOwner}.
	 * @return the logical view name for the create/update form
	 */
	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		OwnerDto ownerDto = findOwner(ownerId);
		model.addAttribute("owner", ownerDto);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists changes to an existing owner. Verifies that the form's owner
	 * ID matches the URL path variable before saving.
	 * @param owner the updated owner data from the submitted form
	 * @param result binding and validation results
	 * @param ownerId the owner's primary key from the URL path
	 * @param redirectAttributes flash attributes for the redirect
	 * @return a redirect to the owner's detail page, or the form view on error
	 */
	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid @ModelAttribute("owner") OwnerDto dto, BindingResult result,
			@PathVariable("ownerId") int ownerId, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		if (!Objects.equals(dto.getId(), ownerId)) {
			result.rejectValue("id", "mismatch", "The owner ID in the form does not match the URL.");
			redirectAttributes.addFlashAttribute("error", "Owner ID mismatch. Please try again.");
			return "redirect:/owners/{ownerId}/edit";
		}

		Owner owner = owners.findById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException(OWNER_NOT_FOUND_MESSAGE + ownerId));
		updateOwnerFromDto(dto, owner);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Owner owner = this.owners.findById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException(
					OWNER_NOT_FOUND_MESSAGE + ownerId + ". Please ensure the ID is correct "));
		mav.addObject("owner", toDto(owner));
		return mav;
	}

}
